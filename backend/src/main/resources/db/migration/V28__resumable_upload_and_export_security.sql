CREATE TABLE file_upload_session (
    id UUID PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(200) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    expected_sha256 CHAR(64),
    business_type VARCHAR(30) NOT NULL,
    business_ref VARCHAR(120) NOT NULL,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    chunk_size INTEGER NOT NULL CHECK (chunk_size > 0),
    total_chunks INTEGER NOT NULL CHECK (total_chunks BETWEEN 1 AND 10000),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING'
        CHECK (status IN ('UPLOADING','COMPLETING','FAILED','COMPLETED','CANCELLED','EXPIRED')),
    uploaded_by BIGINT NOT NULL REFERENCES sys_user(id),
    file_asset_id BIGINT REFERENCES file_asset(id),
    expires_time TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE file_upload_chunk (
    upload_id UUID NOT NULL REFERENCES file_upload_session(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    size_bytes INTEGER NOT NULL CHECK (size_bytes > 0),
    sha256 CHAR(64) NOT NULL,
    gridfs_id VARCHAR(24) NOT NULL UNIQUE,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (upload_id, chunk_index)
);

CREATE INDEX idx_file_upload_session_owner_status ON file_upload_session(uploaded_by,status,expires_time);
CREATE INDEX idx_file_upload_session_expiry ON file_upload_session(status,expires_time);

COMMENT ON TABLE file_upload_session IS '大文件分片断点续传会话；客户端UUID保证初始化幂等';
COMMENT ON TABLE file_upload_chunk IS '已校验SHA-256的GridFS临时分片';
