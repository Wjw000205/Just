CREATE TABLE sys_department (
    id BIGSERIAL PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    dept_code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES sys_department(id),
    dept_path VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    resource_type VARCHAR(20) NOT NULL DEFAULT 'api',
    resource_path VARCHAR(255),
    http_method VARCHAR(10),
    parent_id BIGINT REFERENCES sys_permission(id),
    description VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    secondary_password VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    department_id BIGINT REFERENCES sys_department(id),
    last_login_time TIMESTAMPTZ,
    last_login_ip VARCHAR(50),
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    password_changed_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT,
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(username) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_user_email ON sys_user(email) WHERE deleted = 0 AND email IS NOT NULL;
CREATE INDEX idx_sys_user_department ON sys_user(department_id);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_data_scope (
    id BIGSERIAL PRIMARY KEY,
    scope_code VARCHAR(80) NOT NULL UNIQUE,
    scope_name VARCHAR(120) NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sys_user_data_scope (
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, data_scope_id)
);

CREATE TABLE tpl_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('template', 'fragment')),
    tag VARCHAR(500),
    description TEXT,
    source VARCHAR(100),
    content JSONB NOT NULL,
    schema_definition JSONB,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    creator_id BIGINT NOT NULL REFERENCES sys_user(id),
    creator_name VARCHAR(100) NOT NULL,
    auditor_id BIGINT REFERENCES sys_user(id),
    auditor_name VARCHAR(100),
    audit_time TIMESTAMPTZ,
    audit_status SMALLINT CHECK (audit_status IN (0, 1, 2)),
    audit_comment VARCHAR(500),
    version INTEGER NOT NULL DEFAULT 1,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_tpl_template_search ON tpl_template USING gin(to_tsvector('simple', coalesce(name,'') || ' ' || coalesce(description,'')));
CREATE INDEX idx_tpl_template_content ON tpl_template USING gin(content);

CREATE TABLE data_dataset (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    tags VARCHAR(500),
    field_definition JSONB NOT NULL,
    data_count BIGINT NOT NULL DEFAULT 0,
    storage_size BIGINT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 0 CHECK (status IN (0, 1, 2)),
    creator_id BIGINT NOT NULL REFERENCES sys_user(id),
    creator_name VARCHAR(100) NOT NULL,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    version INTEGER NOT NULL DEFAULT 1,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_data_dataset_search ON data_dataset USING gin(to_tsvector('simple', coalesce(name,'') || ' ' || coalesce(description,'') || ' ' || coalesce(tags,'')));

CREATE TABLE user_favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('template', 'dataset')),
    target_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE TABLE trace_entity (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(40) NOT NULL,
    entity_code VARCHAR(100) NOT NULL,
    entity_name VARCHAR(200) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    source_system VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    source_record_id VARCHAR(120),
    version INTEGER NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0,
    UNIQUE(entity_type, entity_code, data_scope_id)
);
CREATE INDEX idx_trace_entity_properties ON trace_entity USING gin(properties);
CREATE INDEX idx_trace_entity_search ON trace_entity USING gin(to_tsvector('simple', entity_code || ' ' || entity_name));

CREATE TABLE trace_relation (
    id BIGSERIAL PRIMARY KEY,
    from_entity_id BIGINT NOT NULL REFERENCES trace_entity(id),
    to_entity_id BIGINT NOT NULL REFERENCES trace_entity(id),
    relation_type VARCHAR(60) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    effective_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(from_entity_id, to_entity_id, relation_type)
);
CREATE INDEX idx_trace_relation_from ON trace_relation(from_entity_id);
CREATE INDEX idx_trace_relation_to ON trace_relation(to_entity_id);

CREATE TABLE device (
    id BIGSERIAL PRIMARY KEY,
    device_code VARCHAR(100) NOT NULL UNIQUE,
    device_name VARCHAR(200) NOT NULL,
    device_type VARCHAR(60) NOT NULL,
    model VARCHAR(120),
    protocol VARCHAR(60),
    status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    connection_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    data_scope_id BIGINT NOT NULL REFERENCES sys_data_scope(id),
    last_seen_time TIMESTAMPTZ,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ
);

CREATE TABLE device_measurement (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES device(id),
    metric_name VARCHAR(100) NOT NULL,
    metric_value NUMERIC(24,8),
    text_value VARCHAR(1000),
    unit VARCHAR(40),
    quality VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    measured_time TIMESTAMPTZ NOT NULL,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_measurement_device_time ON device_measurement(device_id, measured_time DESC);

CREATE TABLE integration_config (
    id BIGSERIAL PRIMARY KEY,
    system_code VARCHAR(50) NOT NULL UNIQUE,
    system_name VARCHAR(100) NOT NULL,
    system_type VARCHAR(20) NOT NULL CHECK (system_type IN ('MES','PLM','ERP','DEVICE','OTHER')),
    base_url VARCHAR(500),
    auth_type VARCHAR(30) NOT NULL DEFAULT 'HMAC',
    secret_ciphertext TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sync_cursor VARCHAR(200),
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_time TIMESTAMPTZ
);

CREATE TABLE integration_job (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT REFERENCES integration_config(id),
    system_code VARCHAR(50) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(120),
    record_count BIGINT NOT NULL DEFAULT 0,
    error_message TEXT,
    payload_digest VARCHAR(64),
    started_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_time TIMESTAMPTZ,
    UNIQUE(system_code, idempotency_key)
);

CREATE TABLE sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params TEXT,
    response_code INTEGER,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    execution_time BIGINT,
    previous_digest VARCHAR(64),
    record_digest VARCHAR(64) NOT NULL,
    created_time TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_user_time ON sys_audit_log(user_id, created_time DESC);
CREATE INDEX idx_audit_module_time ON sys_audit_log(module, created_time DESC);

CREATE OR REPLACE FUNCTION forbid_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '审计日志为追加写入记录，禁止修改或删除';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_audit_immutable BEFORE UPDATE OR DELETE ON sys_audit_log
FOR EACH ROW EXECUTE FUNCTION forbid_audit_mutation();

INSERT INTO sys_department(id, dept_name, dept_code, dept_path) VALUES
    (1, '平台管理部', 'PLATFORM', '/1'),
    (2, '研发部', 'RND', '/2'),
    (3, '生产部', 'PRODUCTION', '/3'),
    (4, '质量控制部', 'QUALITY', '/4');
SELECT setval('sys_department_id_seq', 4);

INSERT INTO sys_data_scope(id, scope_code, scope_name, scope_type, description) VALUES
    (1, 'GLOBAL', '全局数据域', 'GLOBAL', '系统管理员全局范围'),
    (2, 'RND-ZRNB', '锆铌研发项目', 'PROJECT', '锆铌合金研发数据'),
    (3, 'PROD-JOINT', '人工关节生产', 'PRODUCT_LINE', '人工关节生产数据'),
    (4, 'QC-JOINT', '人工关节质量', 'DEPARTMENT', '人工关节质检数据');
SELECT setval('sys_data_scope_id_seq', 4);

INSERT INTO sys_role(id, role_name, role_code, description, sort_order) VALUES
    (1, '系统管理员', 'ADMIN', '系统全部管理权限', 1),
    (2, '研发人员', 'RND', '研发数据录入、分析与追溯', 2),
    (3, '生产操作人员', 'PRODUCTION', '生产与设备数据采集', 3),
    (4, '质量控制人员', 'QUALITY', '质量数据录入、审核与追溯', 4),
    (5, '企业决策层', 'EXECUTIVE', '汇总看板与报表查看', 5),
    (6, '访客', 'GUEST', '公开数据只读访问', 6);
SELECT setval('sys_role_id_seq', 6);

INSERT INTO sys_permission(permission_name, permission_code) VALUES
    ('用户查看', 'user:read'), ('用户管理', 'user:manage'), ('角色管理', 'role:manage'),
    ('权限分配', 'permission:assign'), ('模板查看', 'template:read'), ('模板创建', 'template:create'),
    ('模板更新', 'template:update'), ('模板删除', 'template:delete'), ('模板发布', 'template:publish'),
    ('模板审核', 'template:audit'), ('数据集查看', 'dataset:read'), ('数据集创建', 'dataset:create'),
    ('数据集更新', 'dataset:update'), ('数据集删除', 'dataset:delete'), ('数据导入', 'dataset:import'),
    ('数据导出', 'dataset:export'), ('追溯查看', 'trace:read'), ('追溯维护', 'trace:write'),
    ('设备查看', 'device:read'), ('设备管理', 'device:manage'), ('系统集成', 'integration:manage'),
    ('审计查看', 'audit:read'), ('审计导出', 'audit:export'), ('看板查看', 'dashboard:read');

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 1, id FROM sys_permission;
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code IN
('template:read','template:create','template:update','template:publish','dataset:read','dataset:create','dataset:update','dataset:import','dataset:export','trace:read','trace:write','dashboard:read');
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN
('dataset:read','dataset:create','dataset:update','dataset:import','trace:read','trace:write','device:read','device:manage','dashboard:read');
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE permission_code IN
('template:read','template:audit','dataset:read','dataset:update','dataset:export','trace:read','trace:write','device:read','audit:read','dashboard:read');
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 5, id FROM sys_permission WHERE permission_code IN
('template:read','dataset:read','dataset:export','trace:read','device:read','dashboard:read');
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT 6, id FROM sys_permission WHERE permission_code IN ('template:read','dataset:read');

INSERT INTO sys_user(id, username, real_name, email, password, status, department_id)
VALUES (1, 'admin', '系统管理员', 'admin@localhost', '$2y$10$V1.zUhQBC9kMcv3RHl57s.K6GBLGyoDxhjWX0HwnTVvmKH99TLgX6', 1, 1);
SELECT setval('sys_user_id_seq', 1);
INSERT INTO sys_user_role(user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_data_scope(user_id, data_scope_id) VALUES (1, 1), (1, 2), (1, 3), (1, 4);

INSERT INTO tpl_template(name, type, tag, description, source, content, schema_definition, published,
                         creator_id, creator_name, audit_status, version, data_scope_id)
VALUES ('增材制造工艺参数采集模板', 'template', '增材制造,工艺参数', '采集3D打印关键工艺参数与操作信息', '系统内置',
        '{"sections":[{"name":"批次信息","fields":[{"key":"batchCode","label":"工艺批次","type":"text","required":true},{"key":"materialBatch","label":"材料批次","type":"text","required":true}]},{"name":"工艺参数","fields":[{"key":"laserPower","label":"激光功率","type":"number","unit":"W"},{"key":"scanSpeed","label":"扫描速度","type":"number","unit":"mm/s"},{"key":"layerThickness","label":"层厚","type":"number","unit":"μm"}]}]}',
        '{"type":"object","required":["batchCode","materialBatch"]}', TRUE, 1, '系统管理员', 1, 1, 2);

INSERT INTO data_dataset(name, description, category, tags, field_definition, status,
                         creator_id, creator_name, data_scope_id)
VALUES ('锆铌合金材料性能测试数据', '存储锆铌合金材料批次、力学性能及测试标准数据', '研发数据', '材料,性能,测试',
        '[{"key":"materialCode","label":"材料编号","type":"string","required":true},{"key":"batchCode","label":"材料批次","type":"string","required":true},{"key":"testStandard","label":"测试标准","type":"string","required":true},{"key":"result","label":"测试结果","type":"number","required":true},{"key":"unit","label":"单位","type":"string","required":true},{"key":"testTime","label":"测试时间","type":"date","required":true}]',
        1, 1, '系统管理员', 2);

INSERT INTO trace_entity(entity_type, entity_code, entity_name, properties, data_scope_id, created_by) VALUES
    ('MATERIAL_BATCH', 'ZRNB-RAW-2026-001', '锆铌合金原材料批次001', '{"supplier":"示范供应商","composition":"Zr-Nb"}', 2, 1),
    ('PROCESS_BATCH', 'AM-2026-001', '增材制造工艺批次001', '{"laserPower":280,"scanSpeed":900}', 2, 1),
    ('PRODUCT', 'JOINT-2026-001', '人工关节样件001', '{"model":"DEMO-ZRNB"}', 3, 1),
    ('QUALITY_INSPECTION', 'QC-2026-001', '样件001质量检测', '{"result":"PASS","standard":"ISO 7206"}', 4, 1);
INSERT INTO trace_relation(from_entity_id, to_entity_id, relation_type, created_by) VALUES
    (1, 2, 'USED_IN', 1), (2, 3, 'PRODUCED', 1), (3, 4, 'INSPECTED_BY', 1);

INSERT INTO device(device_code, device_name, device_type, model, protocol, status, data_scope_id, created_by) VALUES
    ('PRINTER-001', '3D打印设备示范机', '3D_PRINTER', 'DEMO-3DP', 'TCP/IP', 'ONLINE', 3, 1),
    ('OXIDATION-001', '氧化设备示范机', 'OXIDATION', 'DEMO-OX', 'OPC UA', 'OFFLINE', 3, 1),
    ('HARDNESS-001', '显微维氏硬度仪', 'HARDNESS_TESTER', 'HV-1000A', 'FILE', 'ONLINE', 4, 1);

