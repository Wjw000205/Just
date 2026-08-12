ALTER TABLE data_dataset ADD COLUMN importance_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
    CHECK (importance_level IN ('LOW','NORMAL','HIGH','CRITICAL'));

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE dataset_record_workflow DROP CONSTRAINT dataset_record_workflow_status_check;
ALTER TABLE dataset_record_workflow ADD CONSTRAINT dataset_record_workflow_status_check
    CHECK (status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','DELETING','ARCHIVING','ARCHIVED','RESTORING','DELETED'));
ALTER TABLE dataset_record_workflow
    ADD COLUMN delete_event_id BIGINT REFERENCES cross_store_outbox(id),
    ADD COLUMN delete_previous_status VARCHAR(30);

ALTER TABLE file_asset DROP CONSTRAINT file_asset_status_check;
ALTER TABLE file_asset ADD CONSTRAINT file_asset_status_check
    CHECK (status IN ('AVAILABLE','ARCHIVED','DELETED'));

CREATE TABLE lifecycle_retention_policy (
    id BIGSERIAL PRIMARY KEY,
    resource_type VARCHAR(30) NOT NULL DEFAULT 'DATASET_RECORD' CHECK (resource_type='DATASET_RECORD'),
    importance_level VARCHAR(20) NOT NULL CHECK (importance_level IN ('LOW','NORMAL','HIGH','CRITICAL')),
    data_scope_id BIGINT REFERENCES sys_data_scope(id),
    online_days INTEGER NOT NULL CHECK (online_days>=0),
    archive_target VARCHAR(100) NOT NULL DEFAULT 'PRIMARY_BACKUP',
    auto_archive BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    UNIQUE NULLS NOT DISTINCT(resource_type,importance_level,data_scope_id)
);

CREATE TABLE lifecycle_hold (
    id BIGSERIAL PRIMARY KEY,
    resource_type VARCHAR(30) NOT NULL DEFAULT 'DATASET_RECORD' CHECK (resource_type='DATASET_RECORD'),
    resource_ref VARCHAR(160) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    valid_to TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_by BIGINT REFERENCES sys_user(id),
    released_time TIMESTAMPTZ,
    release_reason VARCHAR(1000),
    version INTEGER NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uk_lifecycle_hold_active ON lifecycle_hold(resource_type,resource_ref) WHERE active=TRUE;

CREATE TABLE lifecycle_archive_job (
    id BIGSERIAL PRIMARY KEY,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('MANUAL','AUTO','RETRY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING','COMPLETED','PARTIAL','FAILED')),
    requested_by BIGINT REFERENCES sys_user(id),
    requested_by_name VARCHAR(100) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    started_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_time TIMESTAMPTZ,
    error_message TEXT
);

CREATE TABLE lifecycle_archive_item (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES lifecycle_archive_job(id),
    dataset_id BIGINT NOT NULL REFERENCES data_dataset(id),
    record_id VARCHAR(24) NOT NULL,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    previous_status VARCHAR(30) NOT NULL,
    state VARCHAR(20) NOT NULL CHECK (state IN ('ARCHIVING','ARCHIVED','RESTORING','ONLINE','FAILED','SUPERSEDED')),
    archive_target VARCHAR(100) NOT NULL,
    archive_path TEXT,
    archive_digest VARCHAR(64),
    manifest JSONB NOT NULL DEFAULT '{}'::jsonb,
    search_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    archived_by BIGINT REFERENCES sys_user(id),
    archived_time TIMESTAMPTZ,
    restored_by BIGINT REFERENCES sys_user(id),
    restored_time TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    external_cleanup_pending BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_lifecycle_archive_open_record ON lifecycle_archive_item(dataset_id,record_id)
    WHERE state IN ('ARCHIVING','ARCHIVED','RESTORING');
CREATE INDEX idx_lifecycle_archive_state ON lifecycle_archive_item(state,updated_time DESC);
CREATE INDEX idx_lifecycle_archive_record ON lifecycle_archive_item(dataset_id,record_id);
CREATE INDEX idx_lifecycle_archive_search_data ON lifecycle_archive_item USING gin(search_data jsonb_path_ops);
CREATE INDEX idx_lifecycle_archive_search_text ON lifecycle_archive_item USING gin ((search_data::text) gin_trgm_ops);

INSERT INTO lifecycle_retention_policy(importance_level,online_days,archive_target,auto_archive)
VALUES ('LOW',30,'PRIMARY_BACKUP',FALSE),('NORMAL',365,'PRIMARY_BACKUP',FALSE),
       ('HIGH',1825,'PRIMARY_BACKUP',FALSE),('CRITICAL',36500,'PRIMARY_BACKUP',FALSE);

INSERT INTO sys_permission(permission_name,permission_code) VALUES
    ('生命周期查看','lifecycle:read'),('生命周期策略管理','lifecycle:manage'),
    ('数据归档','lifecycle:archive'),('归档恢复','lifecycle:restore'),('数据保留例外','lifecycle:hold')
ON CONFLICT(permission_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code LIKE 'lifecycle:%' ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='DATA_ADMIN' AND p.permission_code IN ('lifecycle:read','lifecycle:manage','lifecycle:archive','lifecycle:restore','lifecycle:hold')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE lifecycle_archive_item IS 'PG权威归档清单；ZIP备份含Mongo记录及GridFS附件，摘要验证后才能切换ARCHIVED';
COMMENT ON TABLE lifecycle_hold IS '诉讼/偏差/质量调查等保留例外，生效期间自动和手工归档均被阻止';
