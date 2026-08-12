ALTER TABLE sys_audit_log ADD COLUMN signed_payload TEXT;
ALTER TABLE cross_store_outbox ADD COLUMN actor_id BIGINT;
ALTER TABLE cross_store_outbox ADD COLUMN actor_username VARCHAR(50);
CREATE INDEX idx_cross_store_outbox_actor ON cross_store_outbox(actor_id, created_time);
