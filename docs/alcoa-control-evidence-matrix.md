# ALCOA+ 技术控制—测试—证据矩阵

> 版本基线：V33 + BAK-002，2026-08-12  
> 适用范围：当前平台内的模板、数据集记录、设备测量、追溯实体/关系、附件、电子签名和系统审计  
> 判定边界：本文证明当前软件技术控制及其自动化证据，不替代质量体系适用性评估、SOP、人员培训、生产时钟/备份证明或法规负责人签署。

## 1. 使用规则

每项控制均须能从“要求 → 设计/代码 → 自动测试 → 运行证据”双向追踪。发布验收时应保存本矩阵、对应源码版本/制品摘要、Flyway 版本、Surefire XML、导出的审计/签名验真结果及审批记录。若代码、迁移、密钥策略或业务流程发生变化，受影响行必须重新评估并复测。

状态说明：

- **工程闭环**：当前代码存在强制控制，且冻结快照自动测试覆盖代表性正向/负向路径。
- **运行证据待取**：软件控制存在，但生产环境或持续运行证据只能在部署后形成。
- **质量签署待办**：需要质量/法规负责人确定适用性、批准偏差并签署，不能由开发测试代替。

## 2. ALCOA+ 九原则矩阵

| 原则 | 控制 ID | 当前技术控制 | 代码/数据证据 | 自动测试证据 | 状态与剩余证据 |
|---|---|---|---|---|---|
| Attributable 可归因 | DI-ATT-01 | 认证用户、当前角色/数据域、创建/修改/审核/发布人写入权威记录；审计保存 userId、username、操作、模块、目标；设备接入使用独立 HMAC 凭据 | `AuditService.recordAs`；`dataset_record_workflow`；`electronic_signature`；`device_measurement`；V7/V11 | `PlatformIntegrationTest#corePlatformWorksAcrossPostgresMongoAndRedis` 覆盖登录、权限、设备 HMAC、审核/发布签名和审计归因 | 工程闭环；生产账号实名核验和人员离调岗记录待质量/人事证据 |
| Legible 清晰可读 | DI-LEG-01 | 业务字段以结构化 JSON Schema/JSONB 保存；历史版本、签名载荷、审计详情、追溯来源可通过 API/UI 读取；审计支持 CSV/XLSX 导出 | 模板 `content/schema_definition`；Mongo `data`；`TraceService.evidence`；`AuditService.export` | 模板历史、三格式导入导出、审计导出、追溯原始参数/来源及附件证据断言 | 工程闭环；正式打印版式、术语/字典和归档阅读软件版本待业务确认 |
| Contemporaneous 同步记录 | DI-CON-01 | 创建、状态转换、审核、签名和审计在同一请求/事务中记录微秒级时间；通知、跨库写和生命周期任务保留开始/完成/失败时间 | `AuditService`；`RecordWorkflowService.sign`；工作流/Outbox/生命周期时间列 | 阻塞事务的提交可见性、签名时间/来源 IP、注册审核并发、Outbox/归档状态断言 | 工程闭环；生产 NTP/时区配置、时钟漂移告警和校准记录属于运行证据待取 |
| Original 原始 | DI-ORI-01 | 设备原始测量禁止更新/删除；受控记录更正不覆盖审核版本，使用 recordVersion 与 CORRECTING 围栏；模板保存不可覆盖快照；来源系统/来源记录号随追溯实体保存 | V7 不可变触发器；V11/V22；`CrossStoreOutboxService`；`tpl_template_version`；`trace_entity.source_*` | 测量不可变、同键幂等、更正升版/旧版本签名、模板版本、追溯 sourceSystem/sourceRecordId 断言 | 工程闭环；外部仪器原始文件与纸质原件的系统边界、迁移前来源证明待业务签署 |
| Accurate 准确 | DI-ACC-01 | JSON Schema/类型/必填校验；导入逐行错误；文件和归档按 SHA-256 验真；签名绑定规范化记录内容与附件清单摘要；跨库存储有投影/文件对账 | `DatasetService.validateRecord`；`DatasetImportService`；文件/归档服务；`RecordWorkflowService.verifySignature`；两个 Reconciler | 非法字段、三格式 PARTIAL、附件/归档篡改、签名独立摘要重算、PG/Mongo 投影漂移与修复断言 | 工程闭环；量具校准、采集精度、单位/字典主数据批准和业务抽样对账待外部证据 |
| Complete 完整 | DI-COM-01 | 登录、查询、创建、修改、删除、导入导出、审核发布、权限变更、文件和生命周期事件进入统一审计；before/after、原因、失败事件及关联目标按操作保存 | `sys_audit_log`；`QueryAuditInterceptor`；各 Service 的 `audit.record*`；V15 索引/锚点 | 审计筛选/导出、关键更新 before/after、失败独立审计、追溯节点附件/审计聚合断言 | 工程闭环；“所有业务关键事件”清单需由流程所有者逐版本批准，浏览器 E2E 仍待补 |
| Consistent 一致 | DI-CNS-01 | 数据库 FK/唯一/CHECK、状态机和乐观版本 CAS；事务/行锁/advisory lock；跨库 Outbox、租约、代际 fencing 与幂等键；集成本地裁决—持久交付两阶段、同实体交付 gap、不可变凭据快照和遗留 HMAC 失败关闭；稳定分页二级排序 | Flyway V1–V33；工作流、治理、共享、生命周期、集成和 Outbox 服务 | 并发 refresh、审核、同键更正/删除、陈旧版本、共享撤权、生命周期恢复、集成游标/交付 gap、OAuth/HMAC 身份、V32→V33 升级及最终租约断言 | 工程闭环；生产多实例压力、数据库参数基线和时间排序性能证据待取 |
| Enduring 持久 | DI-END-01 | PostgreSQL/Mongo/GridFS 持久卷；审计/签名行及审计锚点禁止 UPDATE/DELETE/TRUNCATE；记录可归档为带清单与摘要的 ZIP 并恢复；停写跨库恢复包以版本化 HMAC、逐文件 SHA-256、Flyway/计数/GridFS 引用与分块清单验真 | V1/V11/V15/V21；`LifecycleService`；`cross_store_backup.py`；Compose 持久卷 | 审计/签名篡改拒绝、归档恢复、跨库包篡改拒绝、错误确认不改库和临时 PG17/Mongo8 破坏恢复演练 | 软件恢复控制工程闭环；独立 NAS/WORM、PITR/实时副本、异地保留和生产 RPO/RTO 仍为运行证据待取 |
| Available 可获得 | DI-AVA-01 | 权限与数据域内可查询/检索/导出记录、审计、签名、追溯和归档项；归档保持搜索投影并支持受控恢复；敏感下载使用一次性票据且不缓存 | Search/Trace/Audit/Lifecycle API；`DatasetExportTicketService`；前端对应视图 | 中文检索/高亮、分页排序、审计导出、正反向追溯、归档搜索/恢复、一次性下载票据断言 | 工程闭环；99.5% 可用性、灾备时可获得性和恢复时长需试运行/演练证明 |

## 3. 横向完整性控制

| 风险 | 强制控制 | 可复核证据 | 当前边界 |
|---|---|---|---|
| 管理员直接改删审计/签名 | PostgreSQL 触发器拒绝行更新、删除和表截断；HMAC 链、同库锚点和历史 keyId 验真 | `POST /api/audits/verify`、签名验真 API、数据库负向测试 | 表 owner/超级用户仍可破坏同库对象；需独立数据库角色和外部锚定/WORM |
| 未审核数据被发布 | 创建者不可自审；PENDING 冻结；签名绑定目标版本/内容/附件；更正必须升版并重新审核 | 工作流状态、签名载荷、审核/发布审计 | 正式职责矩阵和代理审批流程待业务批准 |
| 跨 PostgreSQL/Mongo/GridFS 不一致 | Outbox、状态预占、租约、代际 fencing、幂等补偿、投影/文件巡检；停写恢复包拒绝未收敛状态并保存签名跨库清单 | Outbox/归档/文件对账测试；`test_cross_store_backup_restore.sh` 破坏恢复报告 | 生产故障注入、大规模多实例压力及异地 RPO/RTO 证据待取 |
| 越权读取或篡改证据 | RBAC + 方法权限 + 数据域 + 资源状态 + 选择性共享操作复核；后端为最终安全边界 | 跨域/无权限/撤权/停用域/READ-DOWNLOAD 分离负测 | 正式权限矩阵和业务签字待完成 |
| 敏感操作被盗会话执行 | 二级密码、5 次锁定、一次性 step-up proof，绑定用户/会话/方法/目标/载荷/安全戳 | 删除、导出、授权和共享 proof 的缺失/错绑/重放测试 | 生产身份源/MFA 方案和紧急访问 SOP 待批准 |

## 4. 冻结快照证据

- 2026-08-11 10:17 独立执行 `mvn clean test`：PostgreSQL 17.10、MongoDB 8.0、Redis 8.2 三个真实 Testcontainers；Flyway 空库 V1–V30；`tests=3, failures=0, errors=0, skipped=0`，测试类用时 33.28 秒。
- 2026-08-11 12:39 对 V31 冻结快照执行 `mvn clean test`：同样真实启动 PostgreSQL 17.10、MongoDB 8.0、Redis 8.2；Flyway 空库 V1–V31，并在独立 schema 验证 V9 含遗留死信数据升级至 V31；`tests=4, failures=0, errors=0, skipped=0`，测试类用时 36.14 秒。
- 2026-08-11 13:44—13:45 对 V32 冻结快照执行 `mvn clean test`：Flyway 空库 V1–V32，并在独立 schema 验证 V9→V31→V32；覆盖版本化入/出站 HMAC、OAuth2 RFC 7662 全声明和唯一来源身份、慢正文/超限/503、逐状态 API 安全摘要审计、交付凭据轮换快照及终态擦除；`tests=4, failures=0, errors=0, skipped=0`，测试类用时 33.385 秒。
- 2026-08-11 14:44—14:45 对 V33 冻结快照执行 `mvn clean test`：Flyway 空库 V1–V33，并在独立 schema 按 V9→V31→已发布 V32→V33 分段升级；覆盖遗留强/弱 HMAC 凭据解密验强、失败关闭隔离与轮换恢复、空幂等键修复、陈旧请求 fencing、V32 校验和兼容和最小密钥留存；`tests=4, failures=0, errors=0, skipped=0`，测试类用时 34.319 秒。
- 2026-08-12 03:57 执行独立 BAK-002 破坏恢复演练：临时 PostgreSQL 17/MongoDB 8 容器中，静止门禁、GridFS 分块/跨库引用、HMAC/SHA 篡改、错误确认和双库破坏恢复全部通过；恢复前后 Flyway、计数、业务值与 GridFS file/chunk 一致。
- 2026-08-12 03:57—03:58 再次执行最终 V33 `mvn clean test`：真实 PostgreSQL 17.10、MongoDB 8.0、Redis 8.2，Flyway V1–V33 与分段升级均通过；`tests=4, failures=0, errors=0, skipped=0`，测试类用时 50.784 秒。
- 2026-08-12 03:58 前端 Vue/TypeScript/Vite 8.2.1 生产构建通过，1696 个模块；Nginx 1.28 `nginx -t` 和 Compose 5.4.0 配置展开通过。
- 自动测试集中在一个综合测试类，能证明代表性纵切片和安全边界，但不能推导合同要求的“全部用例通过率 ≥98%”；应继续拆分独立用例并纳入发布流水线。

## 5. 发布前人工证据与签署

以下证据缺一不可，且不因本文存在而自动通过：

1. 质量/法规负责人确认 ALCOA+ 适用记录类型、关键数据字段、原始记录边界和职责分离矩阵。
2. 生产 NTP/时区/时钟漂移、账号实名与培训、权限批准和定期复核记录。
3. 备份、异地/WORM 或等效长期保存、恢复演练、实际 RPO/RTO 和保留期批准。
4. 真实设备/外部系统数据映射、校准/精度、迁移校验和业务抽样对账。
5. 性能、安全、浏览器/Linux、试运行和偏差处置报告，以及最终验证方案/报告签署。

因此 `COMP-001` 当前可表述为“技术控制与证据矩阵已形成，正式运行证据和质量签署待办”；ISO 13485、21 CFR Part 11、GDPR、ASTM/ISO 格式的适用性仍单独归入 `COMP-002`，不得合并宣称合规。
