ALTER TABLE sys_department ADD COLUMN IF NOT EXISTS leader_id BIGINT REFERENCES sys_user(id);
ALTER TABLE sys_department ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sys_department ADD COLUMN IF NOT EXISTS updated_time TIMESTAMPTZ;
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS authorization_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sys_data_scope ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sys_data_scope ADD COLUMN IF NOT EXISTS deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE sys_data_scope ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES sys_user(id);
ALTER TABLE sys_data_scope ADD COLUMN IF NOT EXISTS updated_time TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_department_name_parent
    ON sys_department(COALESCE(parent_id,0),dept_name) WHERE deleted=0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_name
    ON sys_role(role_name) WHERE deleted=0;

CREATE TABLE gov_category (
    id BIGSERIAL PRIMARY KEY,
    category_type VARCHAR(20) NOT NULL CHECK(category_type IN ('SCIENTIFIC','INDUSTRY')),
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    parent_id BIGINT REFERENCES gov_category(id),
    product_codes TEXT[] NOT NULL DEFAULT '{}',
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1 CHECK(status IN (0,1)),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gov_category_code ON gov_category(category_type,category_code) WHERE deleted=0;
CREATE UNIQUE INDEX uk_gov_category_name_parent ON gov_category(category_type,COALESCE(parent_id,0),category_name) WHERE deleted=0;
CREATE INDEX idx_gov_category_parent ON gov_category(parent_id,sort_order,id) WHERE deleted=0;

CREATE TABLE gov_category_mapping (
    scientific_category_id BIGINT NOT NULL REFERENCES gov_category(id),
    industry_category_id BIGINT NOT NULL REFERENCES gov_category(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(scientific_category_id,industry_category_id)
);

ALTER TABLE data_dataset ADD COLUMN IF NOT EXISTS scientific_category_id BIGINT REFERENCES gov_category(id);
ALTER TABLE data_dataset ADD COLUMN IF NOT EXISTS industry_category_id BIGINT REFERENCES gov_category(id);
CREATE INDEX IF NOT EXISTS idx_dataset_scientific_category ON data_dataset(scientific_category_id) WHERE deleted=0;
CREATE INDEX IF NOT EXISTS idx_dataset_industry_category ON data_dataset(industry_category_id) WHERE deleted=0;

CREATE TABLE gov_tag (
    id BIGSERIAL PRIMARY KEY,
    tag_name VARCHAR(80) NOT NULL,
    tag_color VARCHAR(20) NOT NULL DEFAULT '#409EFF',
    tag_group VARCHAR(80) NOT NULL DEFAULT '默认分组',
    status SMALLINT NOT NULL DEFAULT 1 CHECK(status IN (0,1)),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gov_tag_name ON gov_tag(lower(tag_name)) WHERE deleted=0;

INSERT INTO gov_tag(tag_name,created_by)
SELECT min(token),1 FROM (
    SELECT trim(value) token FROM tpl_template t CROSS JOIN LATERAL regexp_split_to_table(coalesce(t.tag,''),',') value WHERE t.deleted=0
    UNION
    SELECT trim(value) token FROM data_dataset d CROSS JOIN LATERAL regexp_split_to_table(coalesce(d.tags,''),',') value WHERE d.deleted=0
) tokens WHERE token<>'' GROUP BY lower(token) ON CONFLICT DO NOTHING;

CREATE TABLE tpl_template_tag (
    template_id BIGINT NOT NULL REFERENCES tpl_template(id),
    tag_id BIGINT NOT NULL REFERENCES gov_tag(id),
    PRIMARY KEY(template_id,tag_id)
);
CREATE TABLE data_dataset_tag (
    dataset_id BIGINT NOT NULL REFERENCES data_dataset(id),
    tag_id BIGINT NOT NULL REFERENCES gov_tag(id),
    PRIMARY KEY(dataset_id,tag_id)
);
INSERT INTO tpl_template_tag(template_id,tag_id)
SELECT DISTINCT t.id,g.id FROM tpl_template t CROSS JOIN LATERAL regexp_split_to_table(coalesce(t.tag,''),',') value JOIN gov_tag g ON lower(g.tag_name)=lower(trim(value)) AND g.deleted=0 WHERE t.deleted=0 AND trim(value)<>'' ON CONFLICT DO NOTHING;
INSERT INTO data_dataset_tag(dataset_id,tag_id)
SELECT DISTINCT d.id,g.id FROM data_dataset d CROSS JOIN LATERAL regexp_split_to_table(coalesce(d.tags,''),',') value JOIN gov_tag g ON lower(g.tag_name)=lower(trim(value)) AND g.deleted=0 WHERE d.deleted=0 AND trim(value)<>'' ON CONFLICT DO NOTHING;

CREATE TABLE gov_dictionary (
    id BIGSERIAL PRIMARY KEY,
    dictionary_name VARCHAR(100) NOT NULL,
    dictionary_code VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    status SMALLINT NOT NULL DEFAULT 1 CHECK(status IN (0,1)),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gov_dictionary_code ON gov_dictionary(dictionary_code) WHERE deleted=0;

CREATE TABLE gov_dictionary_item (
    id BIGSERIAL PRIMARY KEY,
    dictionary_id BIGINT NOT NULL REFERENCES gov_dictionary(id),
    item_label VARCHAR(100) NOT NULL,
    item_value VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1 CHECK(status IN (0,1)),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gov_dictionary_item_value ON gov_dictionary_item(dictionary_id,item_value) WHERE deleted=0;

CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    menu_name VARCHAR(80) NOT NULL,
    menu_code VARCHAR(80) NOT NULL,
    route_path VARCHAR(200),
    icon VARCHAR(40),
    permission_code VARCHAR(100),
    parent_id BIGINT REFERENCES sys_menu(id),
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1 CHECK(status IN (0,1)),
    created_by BIGINT REFERENCES sys_user(id),
    created_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ,
    deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_menu_code ON sys_menu(menu_code) WHERE deleted=0;
CREATE UNIQUE INDEX uk_sys_menu_route ON sys_menu(route_path) WHERE deleted=0 AND route_path IS NOT NULL;
CREATE INDEX idx_sys_menu_parent ON sys_menu(parent_id,sort_order,id) WHERE deleted=0;

CREATE TABLE sys_ui_config (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK(id=1),
    primary_color VARCHAR(20) NOT NULL DEFAULT '#1677a8',
    font_size INTEGER NOT NULL DEFAULT 14 CHECK(font_size BETWEEN 12 AND 20),
    border_radius INTEGER NOT NULL DEFAULT 9 CHECK(border_radius BETWEEN 0 AND 24),
    content_spacing INTEGER NOT NULL DEFAULT 20 CHECK(content_spacing BETWEEN 8 AND 40),
    version INTEGER NOT NULL DEFAULT 1,
    updated_by BIGINT REFERENCES sys_user(id),
    updated_time TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO sys_ui_config(id) VALUES (1) ON CONFLICT DO NOTHING;

INSERT INTO sys_permission(permission_name,permission_code,description) VALUES
    ('治理配置查看','governance:read','查看分类、标签、字典、菜单与界面配置'),
    ('分类管理','classification:manage','维护科学分类、产业分类与映射'),
    ('标签管理','tag:manage','维护数据标签及分组'),
    ('数据字典管理','dictionary:manage','维护数据字典与字典项'),
    ('菜单管理','menu:manage','维护平台动态菜单'),
    ('界面管理','ui:manage','维护全局界面主题参数'),
    ('组织机构管理','organization:manage','维护部门树及负责人'),
    ('数据域管理','data-scope:manage','维护部门、项目和产品线数据域')
ON CONFLICT(permission_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('governance:read','classification:manage','tag:manage','dictionary:manage','menu:manage','ui:manage','organization:manage','data-scope:manage')
ON CONFLICT DO NOTHING;
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='DATA_ADMIN' AND p.permission_code IN ('governance:read','classification:manage','tag:manage','dictionary:manage')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(menu_name,menu_code,route_path,icon,permission_code,sort_order) VALUES
    ('工作台','dashboard','/dashboard','◫','dashboard:read',10),
    ('高级检索','search','/search','⌕',NULL,20),
    ('模板中心','templates','/templates','◇','template:read',30),
    ('数据资产','datasets','/datasets','▦','dataset:read',40),
    ('全链路追溯','trace','/trace','⌘','trace:read',50),
    ('设备与采集','devices','/devices','⌁','device:read',60),
    ('附件中心','files','/files','▧','file:read',70),
    ('数据生命周期','lifecycle','/lifecycle','◷','lifecycle:read',80),
    ('系统集成','integrations','/integrations','⇄','integration:manage',90),
    ('审计中心','audits','/audits','◎','audit:read',100),
    ('系统与共享','admin','/admin','⚙','user:manage|share:read',110),
    ('治理配置','governance','/governance','⚒','governance:read',120)
ON CONFLICT DO NOTHING;

INSERT INTO gov_dictionary(dictionary_name,dictionary_code,description,created_by) VALUES
    ('数据重要性','DATA_IMPORTANCE','生命周期与归档策略的数据重要性选项',1),
    ('设备协议','DEVICE_PROTOCOL','设备接入协议类型',1)
ON CONFLICT DO NOTHING;
INSERT INTO gov_dictionary_item(dictionary_id,item_label,item_value,sort_order,created_by)
SELECT d.id,v.label,v.value,v.sort_order,1 FROM gov_dictionary d CROSS JOIN (VALUES
    ('低','LOW',10),('普通','NORMAL',20),('高','HIGH',30),('关键','CRITICAL',40)
) v(label,value,sort_order) WHERE d.dictionary_code='DATA_IMPORTANCE'
ON CONFLICT DO NOTHING;
INSERT INTO gov_dictionary_item(dictionary_id,item_label,item_value,sort_order,created_by)
SELECT d.id,v.label,v.value,v.sort_order,1 FROM gov_dictionary d CROSS JOIN (VALUES
    ('模拟器','SIMULATOR',5),('HTTP 推送','HTTP',10),('TCP/IP','TCP/IP',20),('Modbus TCP','MODBUS_TCP',30),('OPC UA','OPC_UA',40)
) v(label,value,sort_order) WHERE d.dictionary_code='DEVICE_PROTOCOL'
ON CONFLICT DO NOTHING;
