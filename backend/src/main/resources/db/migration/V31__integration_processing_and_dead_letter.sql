ALTER TABLE integration_config
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN mapping_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE integration_job
    ADD COLUMN source_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN mapped_payload JSONB,
    ADD COLUMN mapping_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN processing_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN mapping_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN initial_mapping_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN initial_processing_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN initial_mapping_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN trust_domain VARCHAR(80),
    ADD COLUMN entity_type VARCHAR(80),
    ADD COLUMN record_key VARCHAR(240),
    ADD COLUMN cursor_value VARCHAR(200),
    ADD COLUMN cursor_before VARCHAR(200),
    ADD COLUMN cursor_after VARCHAR(200),
    ADD COLUMN error_category VARCHAR(60),
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN local_committed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (delivery_status IN ('NONE','PENDING','COMPLETED')),
    ADD COLUMN processing_token UUID,
    ADD COLUMN processing_started_time TIMESTAMPTZ,
    ADD COLUMN updated_time TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE integration_job SET direction='INBOUND' WHERE direction NOT IN ('INBOUND','OUTBOUND');
UPDATE integration_job SET status=CASE upper(status)
    WHEN 'PENDING' THEN 'RECEIVED'
    WHEN 'RETRYING' THEN 'RECEIVED'
    WHEN 'PROCESSING' THEN 'RECEIVED'
    WHEN 'SUCCESS' THEN 'COMPLETED'
    WHEN 'DONE' THEN 'COMPLETED'
    WHEN 'MANUAL' THEN 'MANUAL_REVIEW'
    WHEN 'RECEIVED' THEN 'RECEIVED'
    WHEN 'COMPLETED' THEN 'COMPLETED'
    WHEN 'FAILED' THEN 'FAILED'
    WHEN 'SKIPPED' THEN 'SKIPPED'
    WHEN 'MANUAL_REVIEW' THEN 'MANUAL_REVIEW'
    ELSE 'MANUAL_REVIEW' END;

ALTER TABLE integration_job
    ADD CONSTRAINT ck_integration_job_direction CHECK (direction IN ('INBOUND','OUTBOUND')),
    ADD CONSTRAINT ck_integration_job_status CHECK (status IN ('RECEIVED','PROCESSING','COMPLETED','FAILED','SKIPPED','MANUAL_REVIEW'));

ALTER TABLE integration_dead_letter DROP CONSTRAINT integration_dead_letter_status_check;
ALTER TABLE integration_dead_letter
    ADD CONSTRAINT integration_dead_letter_status_check CHECK (status IN ('PENDING','RETRYING','RESOLVED','MANUAL_REVIEW')),
    ADD COLUMN processing_token UUID,
    ADD COLUMN processing_started_time TIMESTAMPTZ,
    ADD COLUMN last_attempt_time TIMESTAMPTZ,
    ADD COLUMN resolved_by BIGINT REFERENCES sys_user(id),
    ADD COLUMN resolution_note VARCHAR(1000),
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN updated_time TIMESTAMPTZ NOT NULL DEFAULT now();

-- Upgrade safety for V9-era rows: old dead letters could have no job, no due time, or a
-- RETRYING state without a lease. Preserve their original JSON by attaching a recoverable
-- synthetic job instead of letting one legacy row stop the scheduler or disappear from UI.
UPDATE integration_dead_letter d
SET job_id=NULL,status='PENDING',next_retry_time=coalesce(d.next_retry_time,now()),error_message=left(d.error_message||'；遗留作业关联不一致，已重建可恢复作业',1000)
FROM integration_job j
WHERE d.job_id=j.id AND (j.integration_id IS NULL OR j.integration_id<>d.integration_id);

INSERT INTO integration_job(integration_id,system_code,direction,status,idempotency_key,record_count,payload_digest,source_payload,
                            mapping_snapshot,processing_config,mapping_version,initial_mapping_snapshot,initial_processing_config,initial_mapping_version)
SELECT d.integration_id,c.system_code,'INBOUND','FAILED','legacy-dlq-'||d.id,1,
       encode(sha256(convert_to(d.payload::text,'UTF8')),'hex'),d.payload,
       mappings.snapshot,c.config,c.mapping_version,mappings.snapshot,c.config,c.mapping_version
FROM integration_dead_letter d
JOIN integration_config c ON c.id=d.integration_id
CROSS JOIN LATERAL (
    SELECT coalesce(jsonb_agg(jsonb_build_object(
        'id',m.id,'sourceField',m.source_field,'targetField',m.target_field,'transformation',m.transformation,
        'required',m.required,'defaultValue',m.default_value) ORDER BY m.id),'[]'::jsonb) AS snapshot
    FROM integration_field_mapping m WHERE m.integration_id=d.integration_id
) mappings
WHERE d.job_id IS NULL
ON CONFLICT (system_code,idempotency_key) DO NOTHING;

UPDATE integration_dead_letter d SET job_id=j.id
FROM integration_config c,integration_job j
WHERE d.job_id IS NULL AND c.id=d.integration_id AND j.system_code=c.system_code AND j.idempotency_key='legacy-dlq-'||d.id;

UPDATE integration_job j SET integration_id=d.integration_id,system_code=c.system_code,
    source_payload=CASE WHEN j.source_payload='{}'::jsonb THEN d.payload ELSE j.source_payload END,
    mapping_snapshot=mappings.snapshot,processing_config=c.config,mapping_version=c.mapping_version,
    initial_mapping_snapshot=mappings.snapshot,initial_processing_config=c.config,initial_mapping_version=c.mapping_version,
    status=CASE WHEN d.status<>'RESOLVED' THEN 'FAILED' ELSE j.status END,updated_time=now()
FROM integration_dead_letter d JOIN integration_config c ON c.id=d.integration_id
CROSS JOIN LATERAL (
    SELECT coalesce(jsonb_agg(jsonb_build_object(
        'id',m.id,'sourceField',m.source_field,'targetField',m.target_field,'transformation',m.transformation,
        'required',m.required,'defaultValue',m.default_value) ORDER BY m.id),'[]'::jsonb) AS snapshot
    FROM integration_field_mapping m WHERE m.integration_id=d.integration_id
) mappings
WHERE d.job_id=j.id;

-- Quarantine any other V1-era orphan job under a disabled synthetic configuration, then
-- make the ownership invariant explicit for all future code.
INSERT INTO integration_config(system_code,system_name,system_type,auth_type,active,created_by)
SELECT DISTINCT j.system_code,'遗留隔离配置 '||j.system_code,'OTHER','NONE',FALSE,(SELECT min(id) FROM sys_user)
FROM integration_job j
WHERE j.integration_id IS NULL AND NOT EXISTS(SELECT 1 FROM integration_config c WHERE c.system_code=j.system_code)
ON CONFLICT (system_code) DO NOTHING;
UPDATE integration_job j SET integration_id=c.id FROM integration_config c WHERE j.integration_id IS NULL AND c.system_code=j.system_code;

-- A legacy non-terminal job without a dead-letter has no trustworthy source event or
-- processing snapshot. Isolate it for an explicit, audited operator decision instead of
-- automatically processing the default '{}' payload after upgrade.
INSERT INTO integration_dead_letter(integration_id,job_id,event_key,payload,error_message,retry_count,status,next_retry_time)
SELECT j.integration_id,j.id,'legacy-job-'||j.id,j.source_payload,
       'V31升级隔离：遗留非终态作业缺少可验证处理快照，请人工检查后重放',0,'MANUAL_REVIEW',NULL
FROM integration_job j
WHERE j.status IN ('RECEIVED','FAILED','MANUAL_REVIEW')
  AND NOT EXISTS(SELECT 1 FROM integration_dead_letter d WHERE d.job_id=j.id)
ON CONFLICT (integration_id,event_key) DO NOTHING;
UPDATE integration_job j SET status='MANUAL_REVIEW',error_category='LEGACY_REVIEW',
    error_message='V31升级隔离：遗留非终态作业缺少可验证处理快照',updated_time=now()
WHERE j.status IN ('RECEIVED','FAILED','MANUAL_REVIEW')
  AND EXISTS(SELECT 1 FROM integration_dead_letter d WHERE d.job_id=j.id AND d.status='MANUAL_REVIEW');

UPDATE integration_dead_letter SET status='PENDING',next_retry_time=coalesce(next_retry_time,now()),
    processing_token=NULL,processing_started_time=NULL,updated_time=now()
WHERE status='RETRYING' OR (status='PENDING' AND next_retry_time IS NULL);

ALTER TABLE integration_job ALTER COLUMN integration_id SET NOT NULL;
ALTER TABLE integration_dead_letter ALTER COLUMN job_id SET NOT NULL;

CREATE OR REPLACE FUNCTION enforce_integration_dead_letter_job() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM integration_job j WHERE j.id=NEW.job_id AND j.integration_id=NEW.integration_id) THEN
        RAISE EXCEPTION 'dead letter job must belong to the same integration';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER integration_dead_letter_job_guard
BEFORE INSERT OR UPDATE OF integration_id,job_id ON integration_dead_letter
FOR EACH ROW EXECUTE FUNCTION enforce_integration_dead_letter_job();

DROP INDEX idx_integration_dead_letter_pending;
CREATE INDEX idx_integration_dead_letter_pending
    ON integration_dead_letter(status,next_retry_time,id)
    WHERE status IN ('PENDING','RETRYING');
CREATE INDEX idx_integration_job_system_status
    ON integration_job(system_code,status,started_time DESC,id DESC);

COMMENT ON COLUMN integration_config.config IS
    '集成处理配置：cursorField/cursorType(NUMBER|INSTANT|LEXICOGRAPHIC)、maxRetries、retryBaseSeconds、ipWhitelist';
COMMENT ON COLUMN integration_job.source_payload IS '验签后持久化的原始JSON事件；不得在普通响应或审计中回显';
COMMENT ON COLUMN integration_job.mapped_payload IS '按integration_field_mapping执行清洗后的内部规范载荷';
COMMENT ON TABLE integration_dead_letter IS '映射/清洗失败事件的持久死信、指数退避、租约和人工重放状态';

CREATE TABLE integration_processed_record (
    id BIGSERIAL PRIMARY KEY,
    trust_domain VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    record_key VARCHAR(240) NOT NULL,
    integration_id BIGINT NOT NULL REFERENCES integration_config(id),
    source_system_code VARCHAR(50) NOT NULL,
    source_priority INTEGER NOT NULL DEFAULT 0,
    source_version VARCHAR(200),
    conflict_policy VARCHAR(30) NOT NULL,
    mapped_payload JSONB NOT NULL,
    payload_digest VARCHAR(64) NOT NULL,
    job_id BIGINT NOT NULL REFERENCES integration_job(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(trust_domain,entity_type,record_key)
);
CREATE INDEX idx_integration_processed_source ON integration_processed_record(integration_id,updated_time DESC,id DESC);

CREATE TABLE integration_conflict_log (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES integration_config(id),
    job_id BIGINT NOT NULL REFERENCES integration_job(id),
    trust_domain VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    record_key VARCHAR(240) NOT NULL,
    winning_job_id BIGINT REFERENCES integration_job(id),
    decision VARCHAR(30) NOT NULL CHECK (decision IN ('ACCEPTED','SKIPPED')),
    reason VARCHAR(500) NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE integration_retry_attempt (
    id BIGSERIAL PRIMARY KEY,
    dead_letter_id BIGINT NOT NULL REFERENCES integration_dead_letter(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    result VARCHAR(30) NOT NULL CHECK (result IN ('FAILED','RESOLVED','SKIPPED')),
    error_category VARCHAR(60),
    error_message VARCHAR(1000),
    mapping_version INTEGER NOT NULL,
    mapping_snapshot_digest VARCHAR(64) NOT NULL,
    processing_config_digest VARCHAR(64) NOT NULL,
    processing_token UUID NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(dead_letter_id,attempt_number)
);

INSERT INTO sys_permission(permission_name,permission_code) VALUES
    ('集成作业查看','integration:read'),('集成死信重放','integration:retry')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('integration:read','integration:retry')
ON CONFLICT DO NOTHING;

UPDATE sys_menu SET permission_code='integration:read',updated_time=now() WHERE menu_code='integrations';

-- A role that can mutate integrations must also be able to open the read-only console.
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT existing.role_id,read_permission.id
FROM sys_role_permission existing
JOIN sys_permission manage_permission ON manage_permission.id=existing.permission_id AND manage_permission.permission_code='integration:manage'
JOIN sys_permission read_permission ON read_permission.permission_code='integration:read'
ON CONFLICT DO NOTHING;
