ALTER TABLE dataset_record_workflow DROP CONSTRAINT dataset_record_workflow_status_check;
ALTER TABLE dataset_record_workflow ADD CONSTRAINT dataset_record_workflow_status_check
    CHECK (status IN ('DRAFT','PENDING_REVIEW','APPROVED','PUBLISHED','REJECTED','CORRECTING',
                      'DELETING','ARCHIVING','ARCHIVED','RESTORING','DELETED'));

ALTER TABLE dataset_record_workflow
    ADD COLUMN correction_event_id BIGINT REFERENCES cross_store_outbox(id),
    ADD COLUMN correction_previous_status VARCHAR(30);

CREATE UNIQUE INDEX uk_record_correction_event
    ON dataset_record_workflow(correction_event_id)
    WHERE correction_event_id IS NOT NULL;

COMMENT ON COLUMN dataset_record_workflow.correction_event_id IS
    '跨 PostgreSQL/MongoDB 受控更正的围栏事件；CORRECTING 期间禁止提交、审核、附件变更及生命周期转换';
