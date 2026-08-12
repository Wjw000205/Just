ALTER TABLE cross_store_outbox ADD COLUMN aggregate_record_id VARCHAR(64);
ALTER TABLE cross_store_outbox ADD COLUMN request_fingerprint VARCHAR(64);
CREATE INDEX idx_cross_store_outbox_record ON cross_store_outbox(aggregate_id, aggregate_record_id);
