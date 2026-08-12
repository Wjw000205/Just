CREATE TABLE sys_audit_anchor (
    log_id BIGINT PRIMARY KEY,
    record_digest VARCHAR(64) NOT NULL,
    anchored_time TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO sys_audit_anchor(log_id, record_digest, anchored_time)
SELECT id, record_digest, created_time FROM sys_audit_log;

CREATE OR REPLACE FUNCTION anchor_audit_record() RETURNS trigger AS $$
BEGIN
    INSERT INTO sys_audit_anchor(log_id, record_digest, anchored_time)
    VALUES (NEW.id, NEW.record_digest, NEW.created_time);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_anchor
AFTER INSERT ON sys_audit_log
FOR EACH ROW EXECUTE FUNCTION anchor_audit_record();

CREATE TRIGGER trg_audit_no_truncate
BEFORE TRUNCATE ON sys_audit_log
FOR EACH STATEMENT EXECUTE FUNCTION forbid_audit_mutation();

CREATE TRIGGER trg_audit_anchor_immutable_rows
BEFORE UPDATE OR DELETE ON sys_audit_anchor
FOR EACH ROW EXECUTE FUNCTION forbid_audit_mutation();

CREATE TRIGGER trg_audit_anchor_no_truncate
BEFORE TRUNCATE ON sys_audit_anchor
FOR EACH STATEMENT EXECUTE FUNCTION forbid_audit_mutation();

-- 全库审计明细只交给专职审计员；质量角色继续承担业务审核，但不再默认读取跨域审计详情。
DELETE FROM sys_role_permission rp
USING sys_role r, sys_permission p
WHERE rp.role_id=r.id AND rp.permission_id=p.id
  AND r.role_code='QUALITY' AND p.permission_code='audit:read';

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='AUDITOR' AND p.permission_code IN ('audit:read','audit:export')
ON CONFLICT DO NOTHING;

-- 版本表只保存不可覆盖的内容快照；审核、发布、停启状态由不可变审计链记录，避免把快照时状态误当成当前生命周期。
ALTER TABLE tpl_template_version
    DROP COLUMN published,
    DROP COLUMN enabled,
    DROP COLUMN audit_status,
    DROP COLUMN audit_comment;
