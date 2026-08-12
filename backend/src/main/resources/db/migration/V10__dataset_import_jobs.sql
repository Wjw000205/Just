CREATE TABLE data_import_job (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES data_dataset(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING','COMPLETED','PARTIAL','FAILED')),
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_by_name VARCHAR(100) NOT NULL,
    started_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_time TIMESTAMPTZ
);
CREATE INDEX idx_import_job_dataset_time ON data_import_job(dataset_id,started_time DESC);

CREATE TABLE data_import_error (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES data_import_job(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    raw_data JSONB NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_import_error_job ON data_import_error(job_id,row_number);
