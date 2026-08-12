CREATE TABLE tpl_template_version (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES tpl_template(id),
    version INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('template', 'fragment')),
    tag VARCHAR(500),
    description TEXT,
    source VARCHAR(100),
    content JSONB NOT NULL,
    schema_definition JSONB,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_by_name VARCHAR(100) NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(template_id, version)
);

CREATE INDEX idx_template_version_history
    ON tpl_template_version(template_id, version DESC);

INSERT INTO tpl_template_version(
    template_id, version, name, type, tag, description, source, content,
    schema_definition, data_scope_id, change_reason, created_by,
    created_by_name, created_time
)
SELECT id, version, name, type, tag, description, source, content,
       schema_definition, data_scope_id, '迁移现有版本', creator_id,
       creator_name, COALESCE(updated_time, created_time)
FROM tpl_template
ON CONFLICT(template_id, version) DO NOTHING;
