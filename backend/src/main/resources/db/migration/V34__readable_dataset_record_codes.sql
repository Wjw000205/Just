ALTER TABLE data_dataset
    ADD COLUMN record_code_prefix VARCHAR(16);

UPDATE data_dataset
SET record_code_prefix = 'DS' || lpad(id::text,4,'0')
WHERE record_code_prefix IS NULL;

ALTER TABLE data_dataset
    ALTER COLUMN record_code_prefix SET NOT NULL,
    ADD CONSTRAINT data_dataset_record_code_prefix_format
        CHECK (record_code_prefix ~ '^[A-Z][A-Z0-9]{1,15}$');

CREATE UNIQUE INDEX uk_data_dataset_record_code_prefix
    ON data_dataset(record_code_prefix);

ALTER TABLE dataset_record_workflow
    ADD COLUMN business_code VARCHAR(96);

-- 历史 Mongo 记录无法由 Flyway 原地改写，因此在 PG 权威工作流中生成稳定兼容码；
-- API 读取历史文档时使用同一算法展示，后续新记录则直接把业务码写入 Mongo。
UPDATE dataset_record_workflow w
SET business_code = d.record_code_prefix || '-' ||
                    to_char(coalesce(w.record_created_time,w.updated_time) AT TIME ZONE 'Asia/Shanghai','YYYYMMDD') ||
                    '-L' || upper(w.record_id)
FROM data_dataset d
WHERE d.id=w.dataset_id AND w.business_code IS NULL;

CREATE UNIQUE INDEX uk_dataset_record_business_code
    ON dataset_record_workflow(business_code)
    WHERE business_code IS NOT NULL;

CREATE INDEX idx_dataset_record_business_code_lookup
    ON dataset_record_workflow(dataset_id,business_code);

COMMENT ON COLUMN data_dataset.record_code_prefix IS
    '数据集唯一业务码前缀；创建首条记录前确定，记录创建后不可变更';
COMMENT ON COLUMN dataset_record_workflow.business_code IS
    '面向用户的可读业务标识码；Mongo ObjectId仍作为内部技术主键';
