ALTER TABLE data_dataset ADD COLUMN template_id BIGINT REFERENCES tpl_template(id);
ALTER TABLE data_dataset ADD COLUMN template_version INTEGER;
ALTER TABLE data_dataset ADD COLUMN provision_status VARCHAR(20) NOT NULL DEFAULT 'READY';

CREATE TABLE cross_store_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_key UUID NOT NULL UNIQUE,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_time TIMESTAMPTZ
);
CREATE INDEX idx_cross_store_outbox_pending ON cross_store_outbox(status, created_time);

