ALTER TABLE cross_store_outbox
    ADD COLUMN processing_token UUID,
    ADD COLUMN processing_started_time TIMESTAMPTZ;

CREATE INDEX idx_outbox_recovery_lease
    ON cross_store_outbox(status,processing_started_time,created_time)
    WHERE status IN ('PENDING','FAILED','PROCESSING');

ALTER TABLE lifecycle_archive_item
    ADD COLUMN archive_artifact_cleanup_pending BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_lifecycle_artifact_cleanup
    ON lifecycle_archive_item(updated_time,id)
    WHERE archive_artifact_cleanup_pending=TRUE;

COMMENT ON COLUMN cross_store_outbox.processing_token IS '多实例补偿任务租约围栏，完成/失败只能由当前token提交';
COMMENT ON COLUMN lifecycle_archive_item.archive_artifact_cleanup_pending IS '失败归档遗留ZIP/tmp的持久清理门禁';
