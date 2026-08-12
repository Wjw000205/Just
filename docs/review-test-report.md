# 研发与生产大数据平台独立审查与测试报告

> **更新提示（2026-08-12 03:58）**：本文第 1—7 节保留 2026-08-09 的阶段性审查过程，其中旧版本号、旧风险项和早期测试范围不能代表当前 V33+BAK-002 冻结状态。当前需求分类、逐项证据和剩余缺口以 [`current-requirements-alignment.md`](./current-requirements-alignment.md) 为准。最终 V33 主线 `clean test` 使用 PostgreSQL 17.10、MongoDB 8.0、Redis 8.2 三个真实 Testcontainers，Flyway V1–V33 从空库迁移，并以独立 schema 按 V9→V31→已发布 V32→V33 分段验证遗留升级；`PlatformIntegrationTest` 为 tests=4、failures=0、errors=0、skipped=0，测试类用时 50.784 秒。独立 BAK-002 演练另用临时 PostgreSQL 17/MongoDB 8 容器，验证静止门禁、签名/摘要/附件分块与跨库引用校验、错误恢复确认无副作用，以及破坏双库后的完整恢复。Vue/TypeScript/Vite 8.2.1 生产构建、Compose 5.4.0 展开和 Nginx 1.28 配置均通过，仅保留约 798.54 kB 主包告警。尚无浏览器 E2E、真实厂商联调、异地/PITR 或甲方业务验收签字，不以本机证据替代合同验收。
>
> 报告日期：2026-08-09（Asia/Shanghai）  
> 审查对象：当前工作区的 Spring Boot、Vue 3、PostgreSQL、MongoDB、Redis 实现  
> 需求依据：`requirements-baseline.md`、`traceability-matrix.md` 及两份原始 DOCX 需求/实施文档

## 1. 结论

当前版本的核心开发与演示基线 **有条件通过**：后端可编译并在 PostgreSQL、MongoDB、Redis 的真实隔离环境中启动；Flyway V1–V13 可从空库迁移；Testcontainers 核心纵切片、Vue 生产构建以及本报告列出的独立运行态测试均通过。测试结束时没有已知的阻塞性核心路径回归。

当前版本 **不建议直接作为生产验收版本放行**。原因不是已测核心路径失败，而是生产密钥门禁、备份恢复、性能容量、安全扫描、浏览器 E2E、病毒扫描、真实 MES/PLM/ERP/设备联调、法规/知识产权结论等生产门禁尚未闭环。需求中的 98% 用例通过率也不能由当前一个综合后端测试类推导。

## 2. 执行环境与代码基线

| 项目 | 本次验证环境/版本 |
|---|---|
| 操作系统 | macOS 26.5.2（Apple Silicon 本机环境） |
| Java / Maven | Java 17.0.17 / Maven 3.9.16 |
| 后端 | Spring Boot 3.5.16，Java 17，JDBC/Security/Flyway/MongoDB/Redis |
| 前端 | Vue 3.5、TypeScript 5.9、Vite 8、Element Plus 2.14 |
| Node / npm | Node 26.5.0 / npm 11.17.0 |
| 容器运行时 | Docker client 29.7.2 / server 29.5.2，Colima socket |
| 数据服务 | PostgreSQL 17 Alpine、MongoDB 8.0、Redis 8.2 Alpine |
| 数据库迁移 | Flyway V1–V33 |

运行态对抗测试使用独立 PostgreSQL 数据库、独立 MongoDB 数据库、Redis DB 15 和后端端口 18081，避免修改主开发库。Testcontainers 使用临时 PostgreSQL/MongoDB/Redis 容器；测试结束后临时容器由测试框架清理。

## 3. 构建、迁移与自动测试结果

| 检查项 | 结果 | 证据/说明 |
|---|---|---|
| 后端编译 | 通过 | Maven 编译成功；主代码基于 Java 17。 |
| Flyway 空库迁移 | 通过 | Testcontainers 从空库依次执行 V1–V33，共 33 个迁移。 |
| 后端集成测试 | 通过 | 2026-08-12 最终 `PlatformIntegrationTest`: tests=4、failures=0、errors=0、skipped=0、50.784 秒；包含空库纵切片及 V9→V31→已发布 V32→V33 遗留死信/凭据升级测试。 |
| BAK-002 跨库恢复 | 通过 | 专属临时 PG17/Mongo8：非静止/缺分块/断引用拒绝，错误密钥/篡改/错误确认拒绝，双库破坏后 Flyway、计数、业务值和 GridFS 恢复。 |
| 前端类型检查与生产构建 | 通过 | `npm run build` 成功，Vite 8.2.1 转换 1696 个模块。 |
| 前端体积门禁 | 告警 | 主包约 798.44 KB，触发大于 500 KB 的拆包告警。 |
| 前端 lint/组件/E2E | 未配置 | `package.json` 当前只有 dev/build/preview，无 lint、组件测试和 Playwright 脚本。 |

本机 Testcontainers 需要显式使用 Colima socket：

```bash
DOCKER_HOST='unix:///Users/yichu/.colima/default/docker.sock' \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE='/var/run/docker.sock' \
TESTCONTAINERS_RYUK_DISABLED='true' \
mvn test
```

直接执行普通 `mvn test` 时，`@Testcontainers(disabledWithoutDocker = true)` 可能因找不到非默认 Docker socket 而跳过测试；不能只根据 `BUILD SUCCESS` 判断后端测试已执行，必须检查 `skipped=0`。

## 4. 已通过的功能与安全场景

| 模块 | 已验证场景 |
|---|---|
| 认证与会话 | 首次登录强制改密；改密前业务 API 被拒绝；改密后旧 access token 失效；旧密码失效；真实 Redis 并发消费同一 refresh cookie 时严格一个 200、一个 401。 |
| RBAC 与数据域 | 匿名、无权、同域、跨域访问矩阵覆盖数据集、导入作业、文件与工作流代表路径；服务端返回 401/403/404 符合隐藏资源和拒绝访问语义。 |
| 管理员安全 | 禁止停用当前管理员、禁止移除唯一启用管理员的 ADMIN 角色、禁止重复角色分配；失败操作未破坏管理员角色。 |
| 模板与数据集 | 可视化模板创建、版本快照、提交、创建者自审 403、独立审核通过、待审核/发布并发冻结；绑定数据集时继承 content/Schema、未知扩展属性和数值 typed options；已发布/被引用对象保护通过。 |
| 记录写入 | 创建、幂等重放、同键不同载荷 409、乐观版本更正、受控删除及计数一致性；必填字段和数值转换的代表性非法输入被拒绝。 |
| 导入与导出 | JSON、CSV、XLSX 混合有效/无效行均得到 PARTIAL（2 行中成功 1、失败 1）并保留行号/原始值/错误；CSV 支持字段 key 和中文标签；CSV/JSON/XLSX 导出及字段选择通过。 |
| 记录工作流 | DRAFT→PENDING_REVIEW→APPROVED/REJECTED→PUBLISHED；创建者不能自审；错误二次密码不改状态、不生成签名；驳回后必须受控更正升版才可重提；APPROVED/PUBLISHED 写保护通过。 |
| 电子签名与审计 | REVIEW_APPROVE、REVIEW_REJECT、PUBLISH 签名绑定记录版本/人/原因/时间并生成 64 位 HMAC 摘要；签名表和审计表 UPDATE/DELETE 触发器拒绝篡改；干净测试环境审计链 `valid=true`。 |
| 投影一致性与保密性 | Mongo 假阴性状态可由 PostgreSQL 权威状态修复；手工把 PG=REJECTED 的非本人记录在 Mongo 伪造成 PUBLISHED，修复前 records、JSON/CSV 导出和详情仍不泄露，随后修复回 REJECTED。 |
| 提交后投影时序 | 以 `sys_audit_log` 排他锁阻塞发布事务：阻塞时独立连接只见 PG=APPROVED、Mongo=APPROVED；释放锁并提交后才同时出现 PUBLISHED，证明 Mongo 投影未在 PG commit 前发布。 |
| 文件与 GridFS | 上传、授权列表、字节一致下载、受控删除、跨域拒绝；文件读取重新复核当前业务对象数据域，记录/附件存在时数据集数据域冻结；SVG/HTML 强制 attachment，纯文本 inline，响应含 `nosniff` 与 CSP sandbox。 |
| 文件一致性任务 | 缺失 GridFS 内容时 PG 元数据转 DELETED；过期孤儿 GridFS 文件/chunks 被删除；用 500 个健康低位对象加第 501 个高位异常验证双游标跨批推进，健康对象全部保留。 |
| 外部集成可靠性与 API 安全 | 配置/映射 CAS 与接收快照、AES-GCM 密钥不回显、读取前 1MB/限流、版本化入/出站 HMAC、OAuth2 RFC 7662 独立 client_id/issuer/audience/scope/exp/nbf、精确 IP、202 异步接收、迟到唯一事件不丢、信任源/来源版本冲突、503→60/300/1800→恢复、凭据轮换快照、租约/死信/二级确认重放、目标 JSON 篡改对账及逐状态请求/响应摘要审计；模拟端点不代表真实厂商联调。 |
| 设备 | 设备测量幂等、同键变载荷 409、不可变控制、配置脱敏；新配置 AES-GCM 入库，旧明文启动回填后无遗留明文。 |
| 追溯 | 两节点有向关系的 forward/reverse/both 结果为 1/0/1；非法 direction 为 400；数据域代表访问路径通过。 |
| HTTP 契约 | 不支持的方法返回 405；常见非法 ID、缺参和布尔格式返回 400 而不是 500。 |
| 前端分页 | 数据集、模板、管理、审计和记录页面统一读取 `PageResponse.list`；静态检查未再发现对旧 `records` 字段的误用。 |

## 5. 本轮发现并由主开发线程修复、随后复测通过的问题

| 问题 | 修复后独立复测结果 |
|---|---|
| PostgreSQL 17 下可空筛选绑定、`ANDTRUE` 拼接和看板保留字别名导致接口失败 | 主要列表和看板均返回 200。 |
| refresh token 非原子消费可能被并发重放 | Redis `GETDEL` 复测为一个成功、一个失败。 |
| 唯一管理员保护缺少事务级并发约束 | 已使用 PostgreSQL advisory lock；单管理员破坏路径全部被拒绝。 |
| Webhook 并发幂等出现 `[200,500]`，并信任可伪造 XFF | 改为冲突安全写入且只使用 `remoteAddr` 后，并发返回 `[200,200]` 同一 jobId，XFF 不再绕过。 |
| CSV/XLSX 某行转换失败会中断整批导入 | 三种格式现在均逐行报告 PARTIAL 1/1。 |
| 前端分页读取后端不存在的 `records` 字段 | 所有相关页面改用 `list`，接口与静态复查通过。 |
| SVG/HTML/PDF 等 Blob 预览可能绕过服务端响应头隔离 | 服务端主动内容强制下载；前端预览白名单收敛为纯文本和被动图片。 |
| 文件对账器固定 LIMIT 500 可能永久饿死高位异常 | PG/GridFS 两条游标跨 501 对象复测通过。 |
| 同一版本可重复驳回触发签名唯一约束 409 | 驳回后必须先受控更正升版，v1/v2 可分别签名。 |
| Mongo 工作流状态投影可能产生假阳性越权读取 | 普通读取和三种导出增加 PostgreSQL 权威复核；伪 PUBLISHED 对抗测试未泄露。 |
| 工作流状态在事务提交前写 Mongo，存在提前可见风险 | 改为事务 `afterCommit` 投影；阻塞事务时序测试通过。 |
| 模板编辑与审核存在 TOCTOU，并发时可能发布未审核内容 | 模板变更、提交、审核和删除统一使用行锁；待审核与已发布状态在锁内复核。 |
| 记录创建与空数据集改结构/删除可能并发穿透 | 使用 PostgreSQL advisory/session 锁和数据集行锁；待处理 CREATE Outbox 存在时拒绝改结构或删除。 |
| 数据集换域不会同步 Mongo/附件，文件只检查陈旧附件域 | 有记录或附件后冻结换域；附件列表/下载每次重新核对当前业务对象及数据域。 |
| 模板 Schema、date/file 类型和数值/布尔 options 可能丢失或失真 | Schema 未知属性原样保存，date/file 生成合法 JSON Schema，绑定时合并全部约束，options 使用 JSON 数组保持类型。 |
| 失效首次改密会话可能产生路由循环，生产角色无模板权限却强制加载模板 | refresh 失败清理本地身份；模板列表按权限加载，无权限角色明确进入自定义结构模式。 |
| 手机号“先查后插”存在并发重复 | Flyway V13 增加活动用户手机号部分唯一索引，冲突返回 409。 |

## 6. 当前仍存在的代码与架构风险

以下条目是当前版本的有效残余风险，不包含已修复的早期问题。

| 等级 | 风险 | 影响/建议 |
|---|---|---|
| 高 | `application.yml` 仍为数据库、Redis、JWT、审计和数据加密密钥提供开发默认值，且未见生产 profile 的失败启动门禁；`secure-cookies` 默认 false。 | 上线前新增明确的 prod profile/启动校验：缺少或命中开发默认值即拒绝启动，并由密钥管理系统注入；生产必须启用安全 Cookie、HTTPS 与精确 CORS。 |
| 高 | 跨库 Outbox 在 Mongo 写入从未发生时不能根据请求自动重放；工作流投影失败仅依赖进程内游标定时修复，异常被静默吞掉。 | 增加持久重试状态、指数退避、告警/指标和人工重放入口；明确死信与处置 SOP。 |
| 中 | 普通读路径已用 PG 二次校验阻止假阳性泄露，但严重漂移时列表页可能被过滤成短页；PG 的 PUBLISHED 行若 Mongo 文档缺失，`total` 也可能与实际列表不一致。 | 查询时采用可填满页的权威分页策略，或维护可审计的持久发布投影；增加“PG 有、Mongo 无”的一致性指标与测试。 |
| 中 | 电子签名复用了审计密钥，但签名表未记录 keyId，也没有独立验签接口/密钥轮换证据。 | 使用独立签名密钥和 keyId，提供历史密钥验证、签名证据导出及轮换演练；法规团队确认是否满足适用条款。 |
| 中 | 审计验链当前同步读取全表；永久保存规模可能造成内存和响应时间问题。 | 改为分段/流式验链、检查点和后台验证任务，保留可验证的区间报告。 |
| 中 | 追溯 CTE 最终结果有限制，但稠密循环图在递归过程中的资源消耗尚未压测。 | 在 SQL 递归阶段限制访问节点/边，设置超时并执行稠密图容量测试。 |
| 中 | 设备/协议入口仍缺设备专用身份、签名和 nonce/replay 设计；反向代理可信来源尚未形成部署级配置。 | 生产接入前实现每设备凭据或证书、重放窗口；只在可信代理网络启用转发头并做部署测试。 |
| 中 | 文件病毒扫描器未接入；当前只有“要求扫描时安全拒绝上传”的保护。 | 接入隔离区、扫描器、超时/失败策略和恶意样本测试后再允许生产文件上传。 |
| 低 | 前端主包超过 Vite 500 KB 告警，且没有 lint、组件测试和 E2E。 | 路由级拆包；建立 lint、Vitest 和 Playwright，覆盖权限路由、重复提交、刷新和导出。 |

## 7. 未覆盖或受外部条件阻塞的验收项

- 未执行正式的 100 并发、每日 100 万条、p95/p99、长时间稳定性和稠密追溯图性能测试。
- PostgreSQL + MongoDB/GridFS 停写一致恢复和隔离破坏演练已完成；WAL/PITR、实时副本、异地恢复、备份加密保留销毁和生产 RPO/RTO 仍未完成。
- 未执行 SAST、DAST、依赖/镜像漏洞扫描、SBOM、渗透测试以及主流浏览器/Linux 兼容矩阵。
- 未接入真实 MES、PLM、ERP，也未获得真实账号、字段契约、联调网络、对账样本和回写确认。
- 未接入 3D 打印、氧化、三坐标、硬度、金相/厚度、疲劳等厂商 SDK/Modbus/OPC UA 点表和真实设备。
- 尚未完成生产病毒扫描器、浏览器级 PDF 沙箱策略验收、设备时序等全资源生命周期和独立 NAS 恢复演练；已实现的断点续传、附件包、中文全文检索、记录生命周期和选择性共享不再属于未验证项。
- ISO 13485、21 CFR Part 11、GDPR、ALCOA+ 的适用性、SOP、验证签署不能由代码测试替代，需质量/法规人员完成。
- 原需求批注中的目标 Linux/Ubuntu 版本、源码版权/知识产权归属和第三方许可证结论仍未关闭，属于交付阻断输入。

## 8. 上线建议

建议把当前版本作为继续联调和用户演示的基线，但生产发布采用以下门禁：

1. **先关闭高风险配置项**：生产密钥失败启动、安全 Cookie/HTTPS/CORS、持久 Outbox/投影告警、病毒扫描。
2. **建立可重复测试门禁**：CI 中固定 Docker socket，明确断言 `tests>0 && skipped=0`；拆分综合后端测试，补充并发、权限、故障注入和 Playwright E2E。
3. **完成非功能验收**：在约定硬件和数据模型上执行容量/性能/稳定性，完成 BAK-001 的 PITR/异地/RPO-RTO、安全扫描与渗透。
4. **完成外部联调**：拿到 MES/PLM/ERP 和设备的正式契约、账号、样本与网络后再签署 P2 集成验收。
5. **冻结合同输入**：确认 `pq=PostgreSQL`、角色/数据域/共享规则、RPO/RTO、审计期限、电子签名适用范围、Linux 版本、版权和许可证。

在上述门禁完成并形成证据包前，验收状态应保持“开发核心路径已测试”，不应标记为“生产验收通过”。
