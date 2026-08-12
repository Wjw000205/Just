ALTER TABLE device_measurement ADD COLUMN source_event_id UUID;
ALTER TABLE device_measurement ADD COLUMN received_time TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE device_measurement ADD COLUMN created_by BIGINT REFERENCES sys_user(id);
ALTER TABLE device_measurement ADD COLUMN request_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX uk_measurement_source_event ON device_measurement(device_id, source_event_id) WHERE source_event_id IS NOT NULL;
CREATE OR REPLACE FUNCTION forbid_measurement_mutation() RETURNS trigger AS $$
BEGIN RAISE EXCEPTION '设备原始测点为追加写入记录，禁止修改或删除'; END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_measurement_immutable BEFORE UPDATE OR DELETE ON device_measurement
FOR EACH ROW EXECUTE FUNCTION forbid_measurement_mutation();
