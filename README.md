# 研发与生产大数据平台

基于 Spring Boot 3、Vue 3、PostgreSQL、MongoDB/GridFS 和 Redis 的可运行平台实现。PostgreSQL（项目中 `pq` 的对齐解释）负责结构化元数据、权限、状态机和审计；MongoDB 负责动态数据集记录与附件内容；Redis 负责验证码、登录锁定和刷新令牌的一次性消费。

## 当前能力

- 登录验证码、BCrypt 密码、首次强制改密、JWT 短令牌、HttpOnly 刷新令牌、RBAC 与数据域隔离；匿名认证入口具有读取前 16KB 请求体门禁、字段长度约束、IP/全局 Redis 原子限流和脱敏缓存键。
- 默认关闭的可配置自助注册：邮箱/手机一次性验证码、待审账号、管理员二次确认审核与模拟通知验收；真实邮件/短信供应商需生产接入。
- 可视化模板设计、版本快照、发布/独立审核、收藏，以及待审核/已发布模板冻结。
- 从已发布完整模板创建数据集、动态字段校验、MongoDB 记录、受控更正/删除、跨库 Outbox 补偿。
- CSV、JSON、XLSX 逐行混合导入与错误明细；三种格式流式导出和字段选择。
- 草稿 → 待审核 → 通过/驳回 → 发布状态机，禁止自审，密码二次确认和不可变电子签名证据。
- 材料、工艺、批次、产品的正反向图谱追溯。
- 设备登记、加密连接配置、幂等测量采集与不可变原始测量。
- PostgreSQL 授权元数据 + MongoDB GridFS 附件、SHA-256、一致性修复和安全下载。
- MES/PLM/ERP 通用集成：持久异步接收、映射/清洗、信任源裁决、幂等交付、退避死信与对账；外部写入支持版本化 HMAC 或 OAuth2 RFC 7662，并强制来源 IP、独立客户端身份和逐调用安全摘要审计。
- 追加写入审计、HMAC 链验证、密钥版本与轮换验证。
- 停写维护窗口下的 PostgreSQL + MongoDB/GridFS 签名一致恢复包：Flyway/计数/附件引用/分块清单、SHA-256、HMAC 验证与隔离破坏恢复演练。

需求边界与原文冲突见 [需求基线](docs/requirements-baseline.md)，逐项状态见 [可追溯矩阵](docs/traceability-matrix.md) 和 [实现状态](docs/implementation-status.md)。系统设计、接口和用户操作分别见 [架构说明](docs/architecture.md)、[API 指南](docs/api-guide.md)、[用户指南](docs/user-guide.md)。真实 MES/PLM/ERP 调用、病毒扫描器、生产邮件/短信供应商和法规验证需要外部接口/合规输入；当前注册通知使用不泄露验证码的模拟适配器形成零费用自动验收，并保持生产默认关闭，不冒充已经完成真实联调。

## 本地启动

要求：Java 17+、Maven 3.9+、Node.js 22+、Docker/Colima。

```bash
docker-compose up -d

cd backend
RDP_BOOTSTRAP_ADMIN_PASSWORD='ChangeAdminPassword_2026!' mvn spring-boot:run

cd ../frontend
npm ci
npm run dev
```

访问 <http://localhost:5173>。全新数据库首次启动时使用 `admin` 和 `RDP_BOOTSTRAP_ADMIN_PASSWORD` 登录；引导管理员可直接进入系统。若数据库已经初始化，重复设置该变量不会重置管理员密码。

## 容器化启动应用

```bash
cp .env.example .env
# 编辑 .env，替换所有示例密码和密钥
docker-compose -f docker-compose.yml -f docker-compose.app.yml --profile app up -d --build
```

访问 `http://localhost:8088`（可通过 `RDP_WEB_PORT` 修改）。生产环境必须启用 TLS，并将 `RDP_SECURE_COOKIES=true`；完整清单见 [部署与运维手册](docs/deployment-operations.md)。

## 构建和测试

```bash
cd backend
mvn test

cd ../frontend
npm run build
```

在 Colima 下运行 Testcontainers：

```bash
DOCKER_HOST="unix://$HOME/.colima/default/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn test
```

测试从空库执行 Flyway V1–V33，并覆盖 PostgreSQL、MongoDB、Redis、认证与可配置注册审核、权限/数据域与选择性共享、系统治理、模板/数据集/记录工作流、中文检索、分片文件与安全导出、生命周期归档恢复、正反向追溯、设备接入、电子签名、外部集成持久接收/映射/信任裁决/幂等交付/退避死信、入/出站 HMAC、OAuth2 自省、遗留凭据失败关闭和审计链。`./tools/test_cross_store_backup_restore.sh` 另以临时 PostgreSQL 17/MongoDB 8 容器执行跨库签名备份和破坏恢复。当前仍需浏览器 E2E、性能、安全、异地/PITR、真实外部联调和法规验收证据。

## 目录

- `backend/`：Spring Boot API、Flyway 迁移和 Testcontainers 集成测试。
- `frontend/`：Vue 3 + TypeScript + Element Plus 管理端。
- `docs/`：需求基线、可追溯矩阵、审查计划与部署运维说明。
- `docker-compose.yml`：PostgreSQL、MongoDB、Redis。
- `docker-compose.app.yml`：后端与前端容器化叠加配置。
