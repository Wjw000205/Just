CREATE TABLE sys_share_rule (
    id BIGSERIAL PRIMARY KEY,
    resource_type VARCHAR(30) NOT NULL CHECK (resource_type IN ('DATA_SCOPE','DATASET')),
    resource_id BIGINT NOT NULL,
    grantee_type VARCHAR(30) NOT NULL CHECK (grantee_type IN ('USER','ROLE','DEPARTMENT')),
    grantee_id BIGINT NOT NULL,
    operations TEXT[] NOT NULL DEFAULT ARRAY['READ']::TEXT[],
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_to TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','REVOKED','EXPIRED')),
    reason VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_by BIGINT REFERENCES sys_user(id),
    revoked_time TIMESTAMPTZ,
    revoke_reason VARCHAR(1000),
    version INTEGER NOT NULL DEFAULT 1,
    CHECK (resource_id > 0 AND grantee_id > 0),
    CHECK (array_length(operations,1) > 0
           AND operations <@ ARRAY['READ','EXPORT','DOWNLOAD']::TEXT[]
           AND 'READ'=ANY(operations)),
    CHECK (valid_to IS NULL OR valid_to > valid_from),
    CHECK ((status='REVOKED') = (revoked_by IS NOT NULL AND revoked_time IS NOT NULL AND revoke_reason IS NOT NULL))
);

CREATE UNIQUE INDEX uk_share_rule_active_target
    ON sys_share_rule(resource_type,resource_id,grantee_type,grantee_id)
    WHERE status='ACTIVE';
CREATE INDEX idx_share_rule_recipient
    ON sys_share_rule(grantee_type,grantee_id,status,valid_from,valid_to);
CREATE INDEX idx_share_rule_resource
    ON sys_share_rule(resource_type,resource_id,status,valid_from,valid_to);

INSERT INTO sys_permission(permission_name,permission_code) VALUES
    ('共享规则查看','share:read'),('共享规则管理','share:manage'),('共享规则撤销','share:revoke')
ON CONFLICT(permission_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('share:read','share:manage','share:revoke')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE sys_share_rule IS '管理员配置的跨用户、角色、部门只读选择性共享规则；历史规则仅撤销/过期，不物理删除';
COMMENT ON COLUMN sys_share_rule.operations IS '共享允许的只读操作；READ必选，EXPORT/DOWNLOAD可选，永不授予写操作';
