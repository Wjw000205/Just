ALTER TABLE dataset_record_workflow
    ADD COLUMN search_projection_ready BOOLEAN NOT NULL DEFAULT FALSE;

-- V17 已运行期间由 Outbox 写入的非空投影可直接确认；真正的历史空投影继续由 Mongo 回填。
UPDATE dataset_record_workflow
SET search_projection_ready=TRUE
WHERE search_data <> '{}'::jsonb;

CREATE INDEX idx_record_search_projection_pending
    ON dataset_record_workflow(dataset_id,record_id)
    WHERE search_projection_ready=FALSE AND status<>'DELETED';

COMMENT ON COLUMN dataset_record_workflow.search_projection_ready IS
    'Mongo动态记录已完成PG检索投影；未就绪时检索返回503，禁止静默漏数或使用错误时间排序';
