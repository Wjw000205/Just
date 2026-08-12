# 部署与运维手册

## 1. 组件与持久化

| 组件 | 用途 | 默认持久卷 |
|---|---|---|
| PostgreSQL 17 | 用户、权限、模板/数据集元数据、状态机、审计、Outbox | `postgres_data` |
| MongoDB 8 | 动态数据记录、GridFS 附件 | `mongodb_data` |
| Redis 8 | 验证码、登录/二级密码锁定、刷新令牌 | `redis_data` |
| Spring Boot | REST API、业务规则、补偿与一致性任务 | 无状态 |
| Nginx + Vue 3 | 静态管理端和同源 API 反向代理 | 无状态 |

跨 PostgreSQL/MongoDB 写入不假装为分布式 ACID：业务通过幂等键、Outbox 状态和定时补偿保证最终一致性。文件元数据与 GridFS 内容由一致性任务分批巡检。

## 2. 密钥与首次启动

从 `.env.example` 创建 `.env`，使用密码管理器或密钥管理服务生成真实值：

- `RDP_BOOTSTRAP_ADMIN_PASSWORD`：只激活全新库中的禁用管理员，并强制首次改密；不会覆盖已初始化管理员。
- `RDP_JWT_SECRET`：JWT 签名密钥，至少 32 个随机字符。
- `RDP_INTEGRATION_DELIVERY_ALLOWED_HOSTS`：允许平台主动投递规范载荷的目标主机白名单，逗号分隔；空值表示禁止所有主动 HTTP 交付。生产默认仅允许 HTTPS 且拒绝回环、链路本地和 RFC1918 地址。
- `RDP_INTEGRATION_ALLOW_PRIVATE_DELIVERY_HOSTS`：仅受控内网联调可显式设为 `true`，此时才允许 HTTP/私网目标；启用前必须保证目标主机和网络出口均已审批。
- `RDP_INTEGRATION_OAUTH_INTROSPECTION_ALLOWED_HOSTS`：OAuth2 RFC 7662 自省端精确主机白名单；空值禁止 OAuth 自省。生产只列经批准的 HTTPS 身份提供方主机。
- `RDP_INTEGRATION_ALLOW_PRIVATE_OAUTH_HOSTS`：仅隔离联调时允许 HTTP/私网自省端；生产保持 `false`。
- `RDP_SCHEDULING_POOL_SIZE`：后台定时任务线程池，默认 6；避免慢集成适配器独占设备心跳、生命周期和存储巡检调度。
- `RDP_AUDIT_SECRET` / `RDP_AUDIT_KEY_ID`：审计链与电子签名证据密钥/版本。
- `RDP_DATA_ENCRYPTION_KEY`：设备与外部系统凭据的 AES-GCM 主密钥。
- `RDP_AUDIT_PREVIOUS_KEYS`：轮换后保留旧审计校验密钥，格式为 `旧keyId=旧secret;更旧keyId=更旧secret`。
- `RDP_TRUST_PROXY_HEADERS`：是否采信反向代理覆盖后的 `X-Real-IP`。仅在后端不对外暴露且入口代理会清洗该头时启用。
- `RDP_REGISTRATION_ENABLED`：自助注册总开关，默认 `false`；后端会强制执行，不能只依赖前端隐藏入口。
- `RDP_REGISTRATION_CHANNELS`：允许的验证渠道，当前契约为 `EMAIL,PHONE`。
- `RDP_REGISTRATION_NOTIFICATION_ADAPTER`：当前内置 `SIMULATED` 只用于本机自动验收，不会向真实邮箱/手机投递验证码。生产接入真实供应商前不得开启注册。
- `RDP_AUTH_RATE_WINDOW_SECONDS`：匿名验证码/登录固定窗口秒数，默认 600，最小按 60 秒执行。
- `RDP_CAPTCHA_IP_RATE_LIMIT` / `RDP_CAPTCHA_GLOBAL_RATE_LIMIT`：验证码窗口内单 IP 与全局请求上限，默认 300/5000。
- `RDP_LOGIN_IP_RATE_LIMIT` / `RDP_LOGIN_GLOBAL_RATE_LIMIT`：登录窗口内单 IP 与全局请求上限，默认 100/5000。限流依赖 Redis，Redis 故障时认证入口失败关闭并写安全审计。

不要把 `.env`、数据库备份或密钥提交到版本库。密钥轮换前应先做完整备份；数据加密主密钥不能直接丢弃，否则既有密文不可恢复。

## 3. 启动、健康检查和停机

```bash
docker-compose -f docker-compose.yml -f docker-compose.app.yml --profile app up -d --build
docker-compose -f docker-compose.yml -f docker-compose.app.yml --profile app ps
curl -fsS http://localhost:8088/actuator/health
```

常规停机不会删除数据卷：

```bash
docker-compose -f docker-compose.yml -f docker-compose.app.yml --profile app down
```

不要在生产环境使用 `down -v`，它会删除数据库与 Redis 持久卷。

## 4. TLS 与反向代理

生产环境必须由受控入口提供 TLS，并设置：

```dotenv
RDP_SECURE_COOKIES=true
RDP_ALLOWED_ORIGINS=https://data.example.com
RDP_TRUST_PROXY_HEADERS=true
```

默认后端不采信任何客户端转发头。应用 Compose 中后端没有宿主端口，唯一入口 Nginx 会覆盖 `X-Real-IP`，因此显式启用该开关用于电子签名来源记录。若后端另行暴露、或 Nginx 前还有负载均衡器，必须限制可信代理网段并让最外层入口清洗来源头；否则应保持关闭，签名只记录直连地址。

Nginx 对 `/api/auth/` 单独限制请求体为 16KB，后端 Servlet 过滤器还会在 JSON 反序列化前执行同等读取上限；不要在外层代理放宽该路径。登录与验证码 Redis 键只保存账号/IP 的 SHA-256 摘要，避免把任意长或敏感原文扩散到缓存键和审计。

Webhook 的 Nginx 运输上限为 2MB，应用读取门禁为 1MB：正常超限由应用产生带请求 ID 的 413 和安全摘要审计，超过运输上限或连接限流的请求只进入 Nginx 结构化安全日志，不保存正文或凭据。不要把网关拒绝误报成应用 `INTEGRATION_API_CALL` 审计行。

OAuth 自省的解析、连接、响应头和最多 64KB 正文共用 100—5000ms 壁钟预算；主机仍必须位于精确白名单。Java HTTP 客户端不能把预校验的 DNS 地址固定到随后连接，因此生产必须使用受信 DNS，并通过出口代理/防火墙只允许批准的身份提供方地址；应用层白名单不能替代该网络控制。HMAC 密钥、OAuth 自省 client secret 和作业凭据快照均为敏感密文，不得写日志、审计详情或备份说明。

从 V32 或更早版本升级到 V33 时，迁移会先把原先启用的 HMAC 集成置为待验证且暂停接入。应用启动守卫会使用当前数据加密密钥解密并复核强度：合格凭据自动恢复；弱密钥或不可解密密文保持停用并生成 `INTEGRATION_CREDENTIAL_ROTATION_REQUIRED` 审计，相关待交付作业进入人工复核。运维应在“系统集成”中录入新的 32—512 字节强密钥，再按审计说明 rebase；不得通过数据库直接重新启用。

## 5. 备份与恢复

`BAK-002` 使用停写维护窗口形成 PostgreSQL 与 MongoDB/GridFS 的同一逻辑恢复点。工具不会假装两个独立数据库存在分布式快照：创建和恢复前必须停止所有应用写入者；存在未收敛 Outbox、记录转换、分片上传或集成租约时直接拒绝备份。Redis 只保存短期安全状态，不进入耐久恢复包；恢复后必须清空既有 Redis 状态、轮换或复核 JWT 密钥并要求全部用户重新登录。

恢复包包含 PostgreSQL custom dump、MongoDB/GridFS archive、Flyway 版本、关键表/集合计数、PostgreSQL `AVAILABLE` 附件引用与 Mongo GridFS ID 清单、GridFS 分块结构结果、逐文件 SHA-256、HMAC 签名清单和密钥版本。包与操作日志权限固定为 `0600`。HMAC 只提供完整性和来源验证，不提供保密性；生产输出目录必须位于加密磁盘/受控备份介质，并由独立备份账号限制读写。

先从密钥管理器注入独立备份签名密钥，不要复用 JWT、审计或数据加密密钥：

```bash
export RDP_BACKUP_HMAC_KEY_ID='backup-prod-v1'
export RDP_BACKUP_HMAC_KEY='从密钥管理器注入的至少32字节随机密钥'
export RDP_BACKUP_OFFLINE_ATTESTATION='I_CONFIRM_ALL_APPLICATION_WRITERS_ARE_STOPPED'
mkdir -p backup

docker-compose -f docker-compose.yml -f docker-compose.app.yml --profile app stop frontend backend
python3 tools/cross_store_backup.py create \
  --compose-file docker-compose.app.yml \
  --offline-attestation "$RDP_BACKUP_OFFLINE_ATTESTATION" \
  --output "backup/rdp-$(date -u +%Y%m%dT%H%M%SZ).tar" \
  --operation-log backup/operations.jsonl
```

备份完成后先离线校验；校验不连接数据库，也不会修改任何数据：

```bash
python3 tools/cross_store_backup.py verify --bundle backup/rdp-20260812T010000Z.tar
```

恢复是破坏性操作，只能在隔离环境或批准的维护窗口执行。工具会先验证 HMAC、逐文件 SHA-256、附件引用清单以及 PostgreSQL/MongoDB 原生归档可读性；然后要求 `--confirm-restore` 精确等于已签名 manifest 中的 `backupId`，数据库名也必须与 manifest 一致，之后才会 drop/recreate PostgreSQL 并 `--drop` 恢复 MongoDB。示例：

```bash
python3 tools/cross_store_backup.py restore \
  --compose-file docker-compose.app.yml \
  --offline-attestation "$RDP_BACKUP_OFFLINE_ATTESTATION" \
  --bundle backup/rdp-20260812T010000Z.tar \
  --confirm-restore 'manifest中的backupId' \
  --report backup/restore-report.json \
  --operation-log backup/operations.jsonl
```

恢复后工具会再次比较 Flyway 版本、关键表/集合计数、GridFS 分块结构和 PostgreSQL→GridFS 引用；任一不一致均返回失败。随后清除旧 Redis 会话状态、以恢复环境启动应用，并人工复核登录、审计链 `POST /api/audits/verify`、数据集记录、附件实际 SHA-256、追溯图、签名验真和集成作业状态。

仓库内置的破坏性演练只使用脚本自行创建的临时容器，不解析或接触现有 Compose 数据卷：

```bash
./tools/test_cross_store_backup_restore.sh
```

该演练验证非静止状态拒绝、篡改拒绝、错误确认不改库，以及破坏 PostgreSQL/MongoDB/GridFS 后的完整恢复。它闭合 `BAK-002` 的软件恢复语义，但不替代 `BAK-001` 所要求的周全量/日增量、WAL/PITR、实时副本、异地介质、保留销毁策略和生产 RPO/RTO 计时演练。

## 6. 监控与告警

- `/actuator/health`：服务与依赖健康。
- `/actuator/metrics`、`/actuator/prometheus`：连接池、HTTP、JVM 指标。
- 告警重点：Outbox 长期 `FAILED/PENDING`、导入失败率、Webhook 失败、文件一致性修复、匿名认证 413/429、登录/二级密码锁定激增、认证限流 Redis 故障、审计链验证失败、磁盘/连接池水位。
- 定期从只读副本或备份环境执行审计链全量校验，避免验证查询影响主库。

## 7. 上线前门禁

1. 替换全部开发密码和密钥，TLS、备份、恢复演练、日志脱敏和最小权限数据库账号完成。
2. 接入真实病毒扫描适配器后再设置 `RDP_REQUIRE_VIRUS_SCAN=true`；当前设为 `true` 会拒绝上传，防止误认为已扫描。
3. MES/PLM/ERP 必须取得接口契约、测试账号、白名单、重试/补偿语义并在隔离环境联调。
4. 自助注册的软件状态机、Redis 验证码和模拟通知已实现；生产启用前必须接入真实邮件/短信供应商、完成发送配额/退订/隐私与故障演练，并把 `RDP_REGISTRATION_NOTIFICATION_ADAPTER` 切换到经批准的适配器。未接入时保持 `RDP_REGISTRATION_ENABLED=false`。
5. ALCOA+、ISO 13485、21 CFR Part 11 等只完成了技术控制基础，最终适用性、验证方案、SOP 与法规签署需由质量/法规负责人完成。
