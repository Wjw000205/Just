CREATE TABLE dataset_record_workflow (
    dataset_id BIGINT NOT NULL REFERENCES data_dataset(id),
    record_id VARCHAR(24) NOT NULL,
    record_version INTEGER NOT NULL DEFAULT 1,
    owner_id BIGINT NOT NULL REFERENCES sys_user(id),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','DELETED')),
    submitted_by BIGINT,
    submitted_time TIMESTAMPTZ,
    reviewed_by BIGINT,
    reviewed_time TIMESTAMPTZ,
    review_comment VARCHAR(1000),
    published_by BIGINT,
    published_time TIMESTAMPTZ,
    updated_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(dataset_id,record_id)
);
CREATE INDEX idx_record_workflow_status ON dataset_record_workflow(status,updated_time DESC);

CREATE TABLE electronic_signature (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    target_version INTEGER NOT NULL,
    signer_id BIGINT NOT NULL REFERENCES sys_user(id),
    signer_username VARCHAR(50) NOT NULL,
    meaning VARCHAR(50) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    signed_payload TEXT NOT NULL,
    evidence_digest VARCHAR(64) NOT NULL,
    signed_time TIMESTAMPTZ NOT NULL,
    UNIQUE(target_type,target_id,target_version,meaning)
);
CREATE INDEX idx_signature_target ON electronic_signature(target_type,target_id,signed_time DESC);

CREATE TRIGGER trg_signature_immutable BEFORE UPDATE OR DELETE ON electronic_signature
FOR EACH ROW EXECUTE FUNCTION forbid_audit_mutation();

INSERT INTO sys_permission(permission_name,permission_code) VALUES
    ('数据审核','dataset:audit'),('数据发布','dataset:publish')
ON CONFLICT(permission_code) DO NOTHING;

INSERT INTO sys_role(role_name,role_code,description,sort_order) VALUES
    ('审核员','AUDITOR','独立审核模板与数据发布',7),
    ('数据管理员','DATA_ADMIN','数据标准、模板和数据集治理',8)
ON CONFLICT(role_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('dataset:audit','dataset:publish') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='QUALITY' AND p.permission_code IN ('dataset:audit','dataset:publish') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='AUDITOR' AND p.permission_code IN ('template:read','template:audit','dataset:read','dataset:audit','dataset:publish','trace:read','audit:read','file:read','dashboard:read') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='DATA_ADMIN' AND p.permission_code IN ('template:read','template:create','template:update','template:publish','dataset:read','dataset:create','dataset:update','dataset:import','dataset:export','file:read','file:upload','trace:read','dashboard:read') ON CONFLICT DO NOTHING;
