ALTER TABLE device
    ADD COLUMN ingest_secret_ciphertext TEXT,
    ADD COLUMN heartbeat_timeout_seconds INTEGER NOT NULL DEFAULT 120 CHECK (heartbeat_timeout_seconds BETWEEN 10 AND 86400),
    ADD COLUMN adapter_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN poll_interval_seconds INTEGER NOT NULL DEFAULT 30 CHECK (poll_interval_seconds BETWEEN 1 AND 86400),
    ADD COLUMN next_poll_time TIMESTAMPTZ,
    ADD COLUMN last_status_change_time TIMESTAMPTZ,
    ADD COLUMN last_status_reason VARCHAR(500);

ALTER TABLE device_measurement
    ADD COLUMN source_principal VARCHAR(150) NOT NULL DEFAULT 'legacy:user',
    ADD COLUMN source_mode VARCHAR(30) NOT NULL DEFAULT 'USER_JWT'
        CHECK (source_mode IN ('USER_JWT','DEVICE_HMAC','ADAPTER')),
    ADD COLUMN raw_digest VARCHAR(64);

CREATE INDEX idx_device_poll_due ON device(next_poll_time,id)
    WHERE adapter_enabled=TRUE;
CREATE INDEX idx_device_heartbeat ON device(last_seen_time,id)
    WHERE status='ONLINE';
CREATE INDEX idx_measurement_source ON device_measurement(device_id,source_mode,received_time DESC);

COMMENT ON COLUMN device.ingest_secret_ciphertext IS '设备HMAC采集密钥的AES-GCM密文；明文仅在创建/轮换时返回一次';
COMMENT ON COLUMN device.adapter_enabled IS '仅表示已配置的平台适配器自动采集；当前内置SIMULATOR，厂商协议仍需独立适配';
