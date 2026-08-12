# API 使用说明

所有业务响应使用 `{ code, message, data }`；列表的分页字段为 `{ total, pageNum, pageSize, pages, list }`。除登录、验证码、注册、刷新和 Webhook 外，接口要求 `Authorization: Bearer <accessToken>`。记录写入还要求每次用户操作生成并复用一个 UUID `X-Idempotency-Key`。

| 模块 | 主要路径 | 说明 |
|---|---|---|
| 认证 | `/api/auth/captcha`、`registration-settings`、`registration-code`、`register`、`login`、`refresh`、`logout` | 注册默认关闭；验证码绑定渠道和目标、5 分钟单次消费；匿名 POST 请求体上限 16KB，验证码/登录有 IP 与全局窗口限流，超限分别返回 413/429；refresh 通过 HttpOnly Cookie 轮换 |
| 用户 | `/api/user/profile`、`/api/user/password` | 首次登录只能改密/退出 |
| 模板 | `/api/templates`、`/{id}/versions`、`/{id}/publish`、`/{id}/audit`、`/{id}/favorite` | 版本历史、发布、审核、收藏和冻结保护 |
| 数据集 | `/api/datasets`、`/{id}/favorite`、`/{id}/records` | 模板约束绑定、收藏、动态字段、幂等写入、受控更正/删除 |
| 工作流 | `/api/datasets/{id}/records/{recordId}/workflow/*` | `submit`、`review`、`publish` |
| 导入导出 | `/api/datasets/{id}/imports`、`/{id}/export` | CSV/JSON/XLSX；导出用 `format`、`fields` |
| 追溯 | `/api/trace/entities`、`relations`、`graph/{id}`、`entities/{id}/evidence` | `direction=forward|reverse|both`，节点证据按数据域与文件权限裁剪 |
| 设备 | `/api/devices`、`/{id}/measurements` | 测量写入要求幂等键 |
| 文件 | `/api/files`、`/api/files/{id}` | multipart 上传、授权下载/预览、逻辑删除 |
| 集成 | `/api/integrations`、`/{id}/mappings`、`/jobs`、`/dead-letters`、`/dead-letters/{id}/retry`、`/{id}/reconcile`、`/webhook/{systemCode}` | Webhook 支持 `RDP-HMAC-V1` 或 OAuth2 RFC 7662 Bearer；接收返回 202，后台处理/退避/死信状态从管理接口读取；人工重放要求 `integration:retry`、说明和 `PRIVILEGE` 二级确认 |
| 审计 | `/api/audits`、`POST /api/audits/verify` | 只追加查询与全链验证 |
| 管理 | `/api/admin/users`、`options`、`status`、`assignments`、`registrations`、`registrations/{id}/review` | 注册审核要求 `user:manage + permission:assign` 和载荷绑定的 `PRIVILEGE` 二次确认；保证至少一名启用管理员 |

## 外部 Webhook 认证

### HMAC-SHA256（`RDP-HMAC-V1`）

每个系统使用独立且至少 32 个 UTF-8 字节的随机共享密钥。时间戳是 Unix 秒，允许偏差 5 分钟；`systemCode` 使用配置中的规范化大写编码。待签名串必须按 UTF-8 编码，换行是单个 LF（`0x0A`），原始请求体字节在反序列化前保持不变：

```text
RDP-HMAC-V1
POST
/api/integrations/webhook/{SYSTEM_CODE}
{X-Timestamp}
{X-Idempotency-Key}
{原始请求体}
```

使用共享密钥计算 HMAC-SHA256，将 64 位小写十六进制放入 `X-Signature`（也兼容 `sha256=` 前缀）。必须同时发送 `X-Timestamp` 和长度不超过 120 的 `X-Idempotency-Key`。重复幂等键同载荷返回同一个接入作业，不同载荷返回 409；签名不能在不同 `systemCode` 路由间复用。

固定测试向量：密钥 `0123456789abcdef0123456789abcdef`，时间戳 `1786400000`，幂等键 `550e8400-e29b-41d4-a716-446655440000`，系统 `MES_MAIN`，原始请求体 `{"id":"WO-1001","quantity":2}`，期望签名为：

```text
e19a5481c4bebf2fb0220d8f5b6382b7dd91a8a0ef7dcde7456e28c0d106103d
```

### OAuth2 RFC 7662

外部系统先用自身独立 AppID/AppSecret 从其授权服务器取得 access token，然后以 `Authorization: Bearer <token>` 调用 Webhook。平台使用另一组“自省端客户端 ID/密钥”访问 RFC 7662 introspection endpoint；两类客户端身份不得混用。启用 OAuth2 时以下配置全部必填：

- 精确来源 IP 白名单；
- 自省端精确主机白名单、URL 和自省客户端 ID/密钥；
- 外部调用方 `client_id`、`issuer`、`audience` 和至少一个 scope；同一 `issuer + client_id` 只能绑定一个集成系统；
- 100–5000ms 自省壁钟超时。

平台校验 `active`、`exp`、`nbf`、`iss`、`aud`、`client_id` 和 scope；自省解析、连接、响应头及最多 64KB 正文共用同一超时预算，失败时关闭为 503。生产默认仅允许 HTTPS、公网解析和部署白名单内主机；部署方还必须用受信 DNS/出口代理或防火墙固定允许的 IdP 出口，Java 进程内校验不替代网络层防 DNS rebinding。

固定长度和分块 Webhook 请求体均限制为 1MB，Redis 限流发生在 MVC 读取请求体之前。到达应用的每次调用生成 `X-Request-ID`，审计仅保存请求/响应摘要、字节数、耗时、状态码、认证类型和 jobId，不保存 Authorization、密钥或原始业务载荷；Nginx 在应用前拒绝的超大运输报文/连接限流只写无正文、无凭据的结构化安全日志。

配置中的 `trustDomain`、`entityType` 和 `recordKeyField` 定义权威记录命名空间，`sourcePriority`、`sourceVersionField` 和 `conflictPolicy` 定义跨来源裁决。`deliveryMode=HTTP` 时还需提供 `deliveryUrl`，且主机必须出现在部署级 `RDP_INTEGRATION_DELIVERY_ALLOWED_HOSTS` 白名单；生产默认只允许 HTTPS 公网目标。平台先在本地事务内完成配置/游标/信任源裁决和规范记录落地，只有被接受的事件才进入持久 `PENDING` 交付，再以规范化 JSON POST，并发送 `X-Idempotency-Key`、`X-Integration-Job-Id`、`X-Source-System`、`X-Timestamp`、`X-Signature-Version: RDP-HMAC-V1` 和 HMAC `X-Signature`。出站 canonical 为：

```text
RDP-HMAC-V1
POST
{deliveryUrl 的 raw path，含原始 query（若有）}
{X-Source-System}
{X-Timestamp}
{X-Idempotency-Key}
{规范化 JSON 请求体}
```

V32 接收时会为交付作业保存不可变的加密凭据快照，使当前配置可以立即轮换；旧的合格作业仍用原版本完成幂等重试，完成、跳过或明确终结后清除作业内密文。V33 首次启动会对升级前启用的 HMAC 配置先失败关闭再解密验证；弱密钥或不可解密密文保持停用，相关待交付作业清除旧密文并进入人工复核，必须录入强密钥后再 rebase。人工 rebase 可在保留本地裁决载荷的前提下显式刷新当前交付参数和凭据，并留下配置/映射摘要。远端必须校验签名并按幂等键保证单一业务效果。同一信任域/实体/业务键的后续被接受版本会等待前序交付完成；408/425/429/5xx、`Retry-After`（秒数或 HTTP-date）和超时进入持久退避，其他 4xx 进入可审计失败/人工处置。
