CREATE TABLE file_asset (
    id BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256 VARCHAR(64) NOT NULL,
    gridfs_id VARCHAR(24) NOT NULL UNIQUE,
    business_type VARCHAR(40) NOT NULL CHECK (business_type IN ('TEMPLATE','DATASET','TRACE_ENTITY','DEVICE','OTHER')),
    business_ref VARCHAR(120) NOT NULL,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    scan_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONFIGURED',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','DELETED')),
    uploaded_by BIGINT NOT NULL REFERENCES sys_user(id),
    uploaded_by_name VARCHAR(100) NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by BIGINT,
    deleted_time TIMESTAMPTZ
);
CREATE INDEX idx_file_asset_business ON file_asset(business_type,business_ref,created_time DESC) WHERE status='AVAILABLE';
CREATE INDEX idx_file_asset_scope ON file_asset(data_scope_id,created_time DESC) WHERE status='AVAILABLE';

CREATE TABLE integration_field_mapping (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES integration_config(id) ON DELETE CASCADE,
    source_field VARCHAR(200) NOT NULL,
    target_field VARCHAR(200) NOT NULL,
    transformation VARCHAR(60) NOT NULL DEFAULT 'DIRECT',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(1000),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(integration_id,source_field)
);

CREATE TABLE integration_dead_letter (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES integration_config(id),
    job_id BIGINT REFERENCES integration_job(id),
    event_key VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    error_message TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RETRYING','RESOLVED')),
    next_retry_time TIMESTAMPTZ,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_time TIMESTAMPTZ,
    UNIQUE(integration_id,event_key)
);
CREATE INDEX idx_integration_dead_letter_pending ON integration_dead_letter(status,next_retry_time);

ALTER TABLE device ADD COLUMN connection_config_ciphertext TEXT;

INSERT INTO sys_permission(permission_name,permission_code) VALUES
    ('文件查看','file:read'),('文件上传','file:upload'),('文件删除','file:delete')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('file:read','file:upload','file:delete')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code IN ('RND','PRODUCTION','QUALITY') AND p.permission_code IN ('file:read','file:upload')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='EXECUTIVE' AND p.permission_code='file:read'
ON CONFLICT DO NOTHING;
