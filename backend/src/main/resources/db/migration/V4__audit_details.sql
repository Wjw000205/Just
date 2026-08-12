ALTER TABLE sys_audit_log ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;

