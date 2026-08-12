ALTER TABLE tpl_template
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN parent_template_id BIGINT REFERENCES tpl_template(id),
    ADD COLUMN source_version INTEGER,
    ADD COLUMN disabled_by BIGINT REFERENCES sys_user(id),
    ADD COLUMN disabled_time TIMESTAMPTZ,
    ADD COLUMN disabled_reason VARCHAR(500);

ALTER TABLE tpl_template
    ADD CONSTRAINT ck_tpl_template_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    ADD CONSTRAINT ck_tpl_template_disabled_metadata CHECK (
        enabled OR (disabled_by IS NOT NULL AND disabled_time IS NOT NULL AND disabled_reason IS NOT NULL)
    );

UPDATE tpl_template SET visibility = 'PUBLIC' WHERE published = TRUE;

ALTER TABLE tpl_template_version
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN restored_from_version INTEGER,
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN audit_status SMALLINT,
    ADD COLUMN audit_comment VARCHAR(500);

ALTER TABLE tpl_template_version
    ADD CONSTRAINT ck_tpl_template_version_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC'));

UPDATE tpl_template_version v
SET visibility = t.visibility,
    published = t.published,
    enabled = t.enabled,
    audit_status = t.audit_status,
    audit_comment = t.audit_comment
FROM tpl_template t
WHERE t.id = v.template_id;

ALTER TABLE tpl_template_version
    ADD CONSTRAINT ck_tpl_template_version_audit_status CHECK (audit_status IN (0, 1, 2));

CREATE INDEX idx_tpl_template_catalog
    ON tpl_template(visibility, enabled, published, type, created_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_dataset_advanced_search
    ON data_dataset(category, status, created_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_trace_entity_advanced_search
    ON trace_entity(entity_type, created_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_device_advanced_search
    ON device(device_type, status, created_time DESC);
