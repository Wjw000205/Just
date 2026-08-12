ALTER TABLE file_asset DROP CONSTRAINT file_asset_business_type_check;
ALTER TABLE file_asset ADD CONSTRAINT file_asset_business_type_check
    CHECK (business_type IN ('TEMPLATE','DATASET','DATASET_RECORD','TRACE_ENTITY','DEVICE','OTHER'));

COMMENT ON COLUMN file_asset.business_ref IS
    '业务对象标识；DATASET_RECORD 使用规范化的 datasetId:MongoObjectId，读取时重新校验数据域与记录可见性';
