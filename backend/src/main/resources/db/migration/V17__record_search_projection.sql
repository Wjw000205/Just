ALTER TABLE dataset_record_workflow
    ADD COLUMN search_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN record_created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN record_updated_time TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_record_search_visibility
    ON dataset_record_workflow(dataset_id,status,owner_id,record_created_time DESC,record_id);
CREATE INDEX idx_record_search_updated
    ON dataset_record_workflow(dataset_id,record_updated_time DESC,record_id)
    WHERE status<>'DELETED';
CREATE INDEX idx_record_search_data
    ON dataset_record_workflow USING gin(search_data jsonb_path_ops);

COMMENT ON COLUMN dataset_record_workflow.search_data IS
    'Mongo动态记录的只读搜索投影；由跨库Outbox在写成功后更新，PG状态/所有者仍是授权权威';
