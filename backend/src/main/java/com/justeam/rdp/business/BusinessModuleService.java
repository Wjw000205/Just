package com.justeam.rdp.business;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.DataScopeService;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BusinessModuleService {
    private static final Map<String, ModuleDefinition> MODULES = Map.of(
            "materials", new ModuleDefinition("MATERIAL_BATCH", List.of("MATERIAL", "MATERIAL_BATCH", "RAW_MATERIAL")),
            "processes", new ModuleDefinition("PROCESS_BATCH", List.of("PROCESS", "PROCESS_BATCH", "PROCESS_ROUTE", "OPERATION")),
            "products", new ModuleDefinition("PRODUCT", List.of("PRODUCT", "PRODUCT_BATCH", "PART")),
            "performance", new ModuleDefinition("PERFORMANCE", List.of("PERFORMANCE", "TEST_RESULT", "QUALITY_INSPECTION")),
            "rnd-projects", new ModuleDefinition("RND_PROJECT", List.of("RND_PROJECT", "PROJECT")),
            "experiments", new ModuleDefinition("EXPERIMENT", List.of("EXPERIMENT", "LAB_EXPERIMENT")),
            "process-experiments", new ModuleDefinition("PROCESS_EXPERIMENT", List.of("PROCESS_EXPERIMENT")),
            "simulations", new ModuleDefinition("SIMULATION", List.of("SIMULATION", "SIMULATION_TASK"))
    );
    private static final Set<String> ASSET_MODULES = Set.of("assets-overview", "materials", "processes", "products", "performance");
    private static final Set<String> RESEARCH_MODULES = Set.of("rnd-projects", "experiments", "process-experiments", "simulations");

    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final DataScopeService scopes;
    private final AuditService audit;

    public BusinessModuleService(JdbcClient jdbc, JsonSupport json, DataScopeService scopes, AuditService audit) {
        this.jdbc = jdbc;
        this.json = json;
        this.scopes = scopes;
        this.audit = audit;
    }

    public Map<String, Object> module(String module, String keyword, int pageNum, int pageSize) {
        String key = normalizeModule(module);
        requireRead(key);
        int page = Math.max(1, pageNum);
        int size = Math.max(1, Math.min(pageSize, 200));
        if ("assets-overview".equals(key)) return assetOverview();
        if ("trace-history".equals(key)) return auditHistory(true, keyword, page, size);
        if ("system-logs".equals(key)) return auditHistory(false, keyword, page, size);
        ModuleDefinition definition = MODULES.get(key);
        if (definition == null) throw BusinessException.notFound("业务模块不存在");
        return entityModule(key, definition, keyword, page, size);
    }

    public Map<String, Object> record(String module, long id) {
        String key = normalizeModule(module);
        requireRead(key);
        ModuleDefinition definition = requireWritable(key);
        Map<String, Object> record = entity(id, definition);
        scopes.requireRead(((Number) record.get("dataScopeId")).longValue());
        return record;
    }

    @Transactional
    public long create(String module, BusinessModuleController.RecordBody body) {
        String key = normalizeModule(module);
        ModuleDefinition definition = requireWritable(key);
        requireWrite();
        scopes.require(body.dataScopeId());
        validateCode(body.code());
        UserPrincipal user = CurrentUser.require();
        Map<String, Object> properties = normalizedProperties(body);
        try {
            long id = jdbc.sql("""
                    INSERT INTO trace_entity(entity_type,entity_code,entity_name,properties,data_scope_id,
                                             source_system,source_record_id,created_by)
                    VALUES (:type,:code,:name,CAST(:properties AS jsonb),:scope,:source,:sourceId,:actor)
                    RETURNING id
                    """).param("type", definition.writeType()).param("code", normalizedCode(body.code()))
                    .param("name", body.name().trim()).param("properties", json.write(properties))
                    .param("scope", body.dataScopeId()).param("source", normalizedSource(body.sourceSystem()))
                    .param("sourceId", key + ":" + normalizedCode(body.code())).param("actor", user.id())
                    .query(Long.class).single();
            audit.record("BUSINESS_RECORD_CREATE", "TRACE", "创建" + moduleLabel(key) + "业务对象",
                    Map.of("entityId", id, "module", key, "code", normalizedCode(body.code()), "dataScopeId", body.dataScopeId()));
            return id;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new BusinessException(409, "同一数据域内业务编码已存在");
        }
    }

    @Transactional
    public void update(String module, long id, BusinessModuleController.RecordBody body) {
        String key = normalizeModule(module);
        ModuleDefinition definition = requireWritable(key);
        requireWrite();
        if (body.version() == null) throw BusinessException.badRequest("更新必须携带当前版本");
        Map<String, Object> before = entityForUpdate(id, definition);
        scopes.require(((Number) before.get("dataScopeId")).longValue());
        if (((Number) before.get("dataScopeId")).longValue() != body.dataScopeId())
            throw BusinessException.badRequest("业务对象创建后不能跨数据域移动");
        validateCode(body.code());
        int changed;
        try {
            changed = jdbc.sql("""
                    UPDATE trace_entity SET entity_code=:code,entity_name=:name,properties=CAST(:properties AS jsonb),
                        source_system=:source,updated_by=:actor,updated_time=now(),version=version+1
                    WHERE id=:id AND version=:version AND deleted=0
                    """).param("code", normalizedCode(body.code())).param("name", body.name().trim())
                    .param("properties", json.write(normalizedProperties(body))).param("source", normalizedSource(body.sourceSystem()))
                    .param("actor", CurrentUser.require().id()).param("id", id).param("version", body.version()).update();
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new BusinessException(409, "同一数据域内业务编码已存在");
        }
        if (changed != 1) throw new BusinessException(409, "业务对象已被其他用户修改，请刷新后重试");
        audit.record("BUSINESS_RECORD_UPDATE", "TRACE", "更新" + moduleLabel(key) + "业务对象",
                Map.of("entityId", id, "module", key, "before", before, "after", entity(id, definition)));
    }

    @Transactional
    public void delete(String module, long id, int version, String reason) {
        String key = normalizeModule(module);
        ModuleDefinition definition = requireWritable(key);
        requireWrite();
        if (reason == null || reason.isBlank()) throw BusinessException.badRequest("删除原因不能为空");
        Map<String, Object> before = entityForUpdate(id, definition);
        scopes.require(((Number) before.get("dataScopeId")).longValue());
        long relations = jdbc.sql("SELECT count(*) FROM trace_relation WHERE from_entity_id=:id OR to_entity_id=:id")
                .param("id", id).query(Long.class).single();
        if (relations > 0) throw new BusinessException(409, "业务对象已参与追溯关系，不能直接删除");
        long files = jdbc.sql("""
                SELECT count(*) FROM file_asset
                WHERE business_type='TRACE_ENTITY' AND business_ref=:ref AND status='AVAILABLE'
                """).param("ref", Long.toString(id)).query(Long.class).single();
        if (files > 0) throw new BusinessException(409, "业务对象仍有关联附件，请先按受控流程处置附件");
        int changed = jdbc.sql("""
                UPDATE trace_entity SET deleted=1,updated_by=:actor,updated_time=now(),version=version+1
                WHERE id=:id AND version=:version AND deleted=0
                """).param("actor", CurrentUser.require().id()).param("id", id).param("version", version).update();
        if (changed != 1) throw new BusinessException(409, "业务对象已被其他用户修改，请刷新后重试");
        audit.record("BUSINESS_RECORD_DELETE", "TRACE", "删除" + moduleLabel(key) + "业务对象",
                Map.of("entityId", id, "module", key, "reason", reason.trim(), "before", before));
    }

    private Map<String, Object> entityModule(String key, ModuleDefinition definition, String keyword, int page, int size) {
        UserPrincipal user = CurrentUser.require();
        String visibility = visibility("e", user);
        String search = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String where = "e.deleted=0 AND e.entity_type IN (:types) AND " + visibility
                + " AND (CAST(:keyword AS text) IS NULL OR lower(e.entity_code) LIKE lower(concat('%',CAST(:keyword AS text),'%'))"
                + " OR lower(e.entity_name) LIKE lower(concat('%',CAST(:keyword AS text),'%')))";
        JdbcClient.StatementSpec totals = bindScopes(jdbc.sql("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE e.properties<> '{}'::jsonb) AS complete,
                       count(*) FILTER (WHERE upper(coalesce(e.properties->>'status',e.properties->>'stage',''))
                         IN ('PENDING','PENDING_REVIEW','FAILED','ERROR','待复核','异常')) AS issues,
                       count(*) FILTER (WHERE e.created_time>=now()-interval '30 days') AS recent
                FROM trace_entity e WHERE
                """ + where).param("types", definition.types()).param("keyword", search), user);
        Aggregate aggregate = totals.query((rs, row) -> new Aggregate(rs.getLong("total"), rs.getLong("complete"),
                rs.getLong("issues"), rs.getLong("recent"))).single();
        JdbcClient.StatementSpec rows = bindScopes(jdbc.sql("""
                SELECT e.id,e.entity_type,e.entity_code,e.entity_name,e.properties::text AS properties,
                       e.data_scope_id,e.source_system,e.source_record_id,e.version,
                       coalesce(e.updated_time,e.created_time) AS event_time,
                       coalesce(u.real_name,u.username,'系统') AS owner,
                       (SELECT count(*) FROM trace_relation r WHERE r.from_entity_id=e.id OR r.to_entity_id=e.id) AS relation_count,
                       (SELECT count(*) FROM file_asset f WHERE f.business_type='TRACE_ENTITY'
                           AND f.business_ref=e.id::text AND f.status='AVAILABLE') AS file_count
                FROM trace_entity e LEFT JOIN sys_user u ON u.id=e.created_by WHERE
                """ + where + " ORDER BY coalesce(e.updated_time,e.created_time) DESC,e.id DESC LIMIT :limit OFFSET :offset")
                .param("types", definition.types()).param("keyword", search).param("limit", size).param("offset", (page - 1) * size), user);
        List<Map<String, Object>> list = rows.query(this::entityRow).list();
        Map<String, Object> result = baseResult(key, aggregate.total(), page, size);
        result.put("rows", list);
        result.put("metrics", entityMetrics(aggregate));
        return result;
    }

    private Map<String, Object> assetOverview() {
        UserPrincipal user = CurrentUser.require();
        List<Map<String, Object>> domains = new ArrayList<>();
        long material = entityCount(MODULES.get("materials").types(), user);
        long process = entityCount(MODULES.get("processes").types(), user);
        long product = entityCount(MODULES.get("products").types(), user);
        long performance = entityCount(MODULES.get("performance").types(), user);
        long device = user.admin() || user.permissions().contains("device:read") ? scopedCount("device", "TRUE", user) : 0;
        long file = user.admin() || user.permissions().contains("file:read") ? scopedCount("file_asset", "status='AVAILABLE'", user) : 0;
        long dataset = user.admin() || user.permissions().contains("dataset:read")
                ? scopedCount("data_dataset", "deleted=0", user) : 0;
        domains.add(domain("材料", material, entityQuality(MODULES.get("materials").types(), user), "/assets/materials"));
        domains.add(domain("工艺", process, entityQuality(MODULES.get("processes").types(), user), "/assets/processes"));
        domains.add(domain("产品", product, entityQuality(MODULES.get("products").types(), user), "/assets/products"));
        domains.add(domain("性能", performance, entityQuality(MODULES.get("performance").types(), user), "/assets/performance"));
        domains.add(domain("设备", device, 100, "/assets/devices"));
        domains.add(domain("文件", file, 100, "/assets/files"));
        long total = material + process + product + performance + device + file + dataset;
        long complete = entityComplete(user);
        long entities = material + process + product + performance;
        long issues = entityIssues(user);
        long recent = entityRecent(user);
        Map<String, Object> result = baseResult("assets-overview", total, 1, 20);
        result.put("domains", domains);
        result.put("sources", sourceDistribution(user));
        result.put("rows", recentEntities(user));
        result.put("metrics", List.of(
                metric("数据资产", number(total), "实时汇总", "cyan"),
                metricUnit("完整度", entities == 0 ? "100" : Long.toString(Math.round(complete * 100.0 / entities)), "%", "mint"),
                metric("待治理问题", number(issues), "空属性或待复核", "amber"),
                metric("近30天新增", number(recent), "按可见数据域统计", "blue")
        ));
        return result;
    }

    private Map<String, Object> auditHistory(boolean traceOnly, String keyword, int page, int size) {
        UserPrincipal user = CurrentUser.require();
        boolean privileged = user.admin() || user.permissions().contains("audit:read");
        String search = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String where = (traceOnly ? "a.module='TRACE'" : "TRUE")
                + (privileged ? "" : " AND a.user_id=:actor")
                + " AND (CAST(:keyword AS text) IS NULL OR lower(a.description) LIKE lower(concat('%',CAST(:keyword AS text),'%'))"
                + " OR lower(a.operation_type) LIKE lower(concat('%',CAST(:keyword AS text),'%'))"
                + " OR lower(a.username) LIKE lower(concat('%',CAST(:keyword AS text),'%')))";
        JdbcClient.StatementSpec totalSpec = jdbc.sql("SELECT count(*) FROM sys_audit_log a WHERE " + where).param("keyword", search);
        JdbcClient.StatementSpec metricSpec = jdbc.sql("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE a.operation_type LIKE '%FAIL%' OR a.operation_type LIKE '%ERROR%') AS errors,
                       count(*) FILTER (WHERE a.created_time>=current_date) AS today,
                       count(DISTINCT a.user_id) AS actors
                FROM sys_audit_log a WHERE
                """ + where).param("keyword", search);
        JdbcClient.StatementSpec rowSpec = jdbc.sql("""
                SELECT a.id,a.username,a.operation_type,a.module,a.description,a.created_time
                FROM sys_audit_log a WHERE
                """ + where + " ORDER BY a.created_time DESC,a.id DESC LIMIT :limit OFFSET :offset")
                .param("keyword", search).param("limit", size).param("offset", (page - 1) * size);
        if (!privileged) {
            totalSpec = totalSpec.param("actor", user.id());
            metricSpec = metricSpec.param("actor", user.id());
            rowSpec = rowSpec.param("actor", user.id());
        }
        long total = totalSpec.query(Long.class).single();
        Aggregate logs = metricSpec.query((rs, row) -> new Aggregate(rs.getLong("total"), rs.getLong("actors"),
                rs.getLong("errors"), rs.getLong("today"))).single();
        List<Map<String, Object>> rows = rowSpec.query((rs, row) -> auditRow(rs, traceOnly)).list();
        String key = traceOnly ? "trace-history" : "system-logs";
        Map<String, Object> result = baseResult(key, total, page, size);
        result.put("rows", rows);
        result.put("metrics", List.of(
                metric(traceOnly ? "追溯记录" : "日志总量", number(logs.total()), "不可变审计源", "cyan"),
                metric("今日事件", number(logs.recent()), "截至当前", "mint"),
                metric("异常 / 失败", number(logs.issues()), "需关注", "amber"),
                metric("涉及用户", number(logs.complete()), "去重主体", "blue")
        ));
        return result;
    }

    private Map<String, Object> auditRow(ResultSet rs, boolean traceOnly) throws SQLException {
        String operation = rs.getString("operation_type");
        String module = rs.getString("module");
        String description = rs.getString("description");
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id"));
        value.put("code", (traceOnly ? "TRACE-" : "LOG-") + rs.getLong("id"));
        value.put("name", description);
        value.put("type", operation);
        value.put("status", failure(operation) ? "异常" : "成功");
        value.put("source", module);
        value.put("owner", rs.getString("username"));
        value.put("time", time(rs.getObject("created_time")));
        value.put("level", failure(operation) ? "ERROR" : operation.contains("WARN") ? "WARN" : "INFO");
        value.put("service", module.toLowerCase(Locale.ROOT));
        value.put("message", description);
        return value;
    }

    private Map<String, Object> entity(long id, ModuleDefinition definition) {
        return jdbc.sql("""
                SELECT e.id,e.entity_type,e.entity_code,e.entity_name,e.properties::text AS properties,
                       e.data_scope_id,e.source_system,e.source_record_id,e.version,
                       coalesce(e.updated_time,e.created_time) AS event_time,
                       coalesce(u.real_name,u.username,'系统') AS owner,
                       (SELECT count(*) FROM trace_relation r WHERE r.from_entity_id=e.id OR r.to_entity_id=e.id) AS relation_count,
                       (SELECT count(*) FROM file_asset f WHERE f.business_type='TRACE_ENTITY'
                           AND f.business_ref=e.id::text AND f.status='AVAILABLE') AS file_count
                FROM trace_entity e LEFT JOIN sys_user u ON u.id=e.created_by
                WHERE e.id=:id AND e.deleted=0 AND e.entity_type IN (:types)
                """).param("id", id).param("types", definition.types()).query(this::entityRow).optional()
                .orElseThrow(() -> BusinessException.notFound("业务对象不存在"));
    }

    private Map<String, Object> entityForUpdate(long id, ModuleDefinition definition) {
        return jdbc.sql("""
                SELECT e.id,e.entity_type,e.entity_code,e.entity_name,e.properties::text AS properties,
                       e.data_scope_id,e.source_system,e.source_record_id,e.version,
                       coalesce(e.updated_time,e.created_time) AS event_time,
                       coalesce(u.real_name,u.username,'系统') AS owner,0 AS relation_count,0 AS file_count
                FROM trace_entity e LEFT JOIN sys_user u ON u.id=e.created_by
                WHERE e.id=:id AND e.deleted=0 AND e.entity_type IN (:types) FOR UPDATE OF e
                """).param("id", id).param("types", definition.types()).query(this::entityRow).optional()
                .orElseThrow(() -> BusinessException.notFound("业务对象不存在"));
    }

    private Map<String, Object> entityRow(ResultSet rs, int row) throws SQLException {
        Map<String, Object> properties = json.map(rs.getString("properties"));
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id"));
        value.put("code", rs.getString("entity_code"));
        value.put("name", rs.getString("entity_name"));
        value.put("type", typeLabel(rs.getString("entity_type")));
        value.put("entityType", rs.getString("entity_type"));
        value.put("status", status(properties));
        value.put("source", rs.getString("source_system"));
        value.put("owner", rs.getString("owner"));
        value.put("time", time(rs.getObject("event_time")));
        value.put("dataScopeId", rs.getLong("data_scope_id"));
        value.put("sourceRecordId", rs.getString("source_record_id"));
        value.put("version", rs.getInt("version"));
        value.put("properties", properties);
        value.put("relationCount", rs.getLong("relation_count"));
        value.put("fileCount", rs.getLong("file_count"));
        return value;
    }

    private long entityCount(List<String> types, UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("SELECT count(*) FROM trace_entity e WHERE e.deleted=0 AND e.entity_type IN (:types) AND " + visibility("e", user))
                .param("types", types), user);
        return spec.query(Long.class).single();
    }

    private int entityQuality(List<String> types, UserPrincipal user) {
        String visible = visibility("e", user);
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("""
                SELECT count(*) AS total,count(*) FILTER (WHERE e.properties<>'{}'::jsonb) AS complete
                FROM trace_entity e WHERE e.deleted=0 AND e.entity_type IN (:types) AND
                """ + visible).param("types", types), user);
        Aggregate value = spec.query((rs, row) -> new Aggregate(rs.getLong("total"), rs.getLong("complete"), 0, 0)).single();
        return value.total() == 0 ? 100 : (int) Math.round(value.complete() * 100.0 / value.total());
    }

    private long entityComplete(UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("SELECT count(*) FROM trace_entity e WHERE e.deleted=0 AND e.properties<>'{}'::jsonb AND " + visibility("e", user)), user);
        return spec.query(Long.class).single();
    }

    private long entityIssues(UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("""
                SELECT count(*) FROM trace_entity e WHERE e.deleted=0 AND
                (e.properties='{}'::jsonb OR upper(coalesce(e.properties->>'status',e.properties->>'stage',''))
                    IN ('PENDING','PENDING_REVIEW','FAILED','ERROR','待复核','异常')) AND
                """ + visibility("e", user)), user);
        return spec.query(Long.class).single();
    }

    private long entityRecent(UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("SELECT count(*) FROM trace_entity e WHERE e.deleted=0 AND e.created_time>=now()-interval '30 days' AND " + visibility("e", user)), user);
        return spec.query(Long.class).single();
    }

    private long scopedCount(String table, String predicate, UserPrincipal user) {
        if (!Set.of("device", "file_asset", "data_dataset").contains(table)) throw new IllegalArgumentException("table");
        String visible = user.admin() ? "TRUE" : user.dataScopes().isEmpty() ? "FALSE" : "data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec spec = jdbc.sql("SELECT count(*) FROM " + table + " WHERE " + predicate + " AND " + visible);
        if (!user.admin() && !user.dataScopes().isEmpty()) spec = spec.param("scopes", user.dataScopes());
        return spec.query(Long.class).single();
    }

    private List<Map<String, Object>> recentEntities(UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("""
                SELECT e.id,e.entity_type,e.entity_code,e.entity_name,e.properties::text AS properties,
                       e.data_scope_id,e.source_system,e.source_record_id,e.version,
                       coalesce(e.updated_time,e.created_time) AS event_time,
                       coalesce(u.real_name,u.username,'系统') AS owner,
                       (SELECT count(*) FROM trace_relation r WHERE r.from_entity_id=e.id OR r.to_entity_id=e.id) AS relation_count,
                       (SELECT count(*) FROM file_asset f WHERE f.business_type='TRACE_ENTITY'
                           AND f.business_ref=e.id::text AND f.status='AVAILABLE') AS file_count
                FROM trace_entity e LEFT JOIN sys_user u ON u.id=e.created_by
                WHERE e.deleted=0 AND
                """ + visibility("e", user) + " ORDER BY e.created_time DESC,e.id DESC LIMIT 10"), user);
        return spec.query(this::entityRow).list();
    }

    private List<Map<String, Object>> sourceDistribution(UserPrincipal user) {
        JdbcClient.StatementSpec spec = bindScopes(jdbc.sql("""
                SELECT e.source_system,count(*) AS count FROM trace_entity e
                WHERE e.deleted=0 AND
                """ + visibility("e", user) + " GROUP BY e.source_system ORDER BY count DESC LIMIT 6"), user);
        List<Map<String, Object>> rows = spec.query((rs, row) -> Map.<String, Object>of(
                "name", rs.getString("source_system"), "count", rs.getLong("count"))).list();
        long total = rows.stream().mapToLong(row -> ((Number) row.get("count")).longValue()).sum();
        String[] colors = {"#39d9f2", "#4f82ff", "#43d6a9", "#9a6dff", "#e8a94c", "#ee6d8d"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = new LinkedHashMap<>(rows.get(index));
            row.put("value", total == 0 ? 0 : Math.round(((Number) row.get("count")).longValue() * 100.0 / total));
            row.put("color", colors[index]);
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> domain(String name, long value, int quality, String route) {
        return Map.of("name", name, "value", number(value), "quality", quality, "route", route);
    }

    private Map<String, Object> baseResult(String module, long total, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", module);
        result.put("source", "LIVE");
        result.put("total", total);
        result.put("pageNum", page);
        result.put("pageSize", size);
        result.put("pages", size == 0 ? 0 : (total + size - 1) / size);
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }

    private List<Map<String, Object>> entityMetrics(Aggregate aggregate) {
        long completeness = aggregate.total() == 0 ? 100 : Math.round(aggregate.complete() * 100.0 / aggregate.total());
        return List.of(
                metric("对象总量", number(aggregate.total()), "实时数据", "cyan"),
                metricUnit("数据完整度", Long.toString(completeness), "%", "mint"),
                metric("异常 / 待办", number(aggregate.issues()), "需关注", "amber"),
                metric("近30天新增", number(aggregate.recent()), "当前数据域", "blue")
        );
    }

    private Map<String, Object> metric(String label, String value, String trend, String tone) {
        return Map.of("label", label, "value", value, "trend", trend, "tone", tone);
    }

    private Map<String, Object> metricUnit(String label, String value, String unit, String tone) {
        return Map.of("label", label, "value", value, "unit", unit, "trend", "实时计算", "tone", tone);
    }

    private String visibility(String alias, UserPrincipal user) {
        return user.admin() ? "TRUE" : user.dataScopes().isEmpty() ? "FALSE" : alias + ".data_scope_id IN (:scopes)";
    }

    private JdbcClient.StatementSpec bindScopes(JdbcClient.StatementSpec spec, UserPrincipal user) {
        if (!user.admin() && !user.dataScopes().isEmpty()) return spec.param("scopes", user.dataScopes());
        return spec;
    }

    private void requireRead(String module) {
        UserPrincipal user = CurrentUser.require();
        if (user.admin()) return;
        if ("system-logs".equals(module) && user.permissions().contains("audit:read")) return;
        if ("trace-history".equals(module) && user.permissions().contains("trace:read")) return;
        if ((ASSET_MODULES.contains(module) || RESEARCH_MODULES.contains(module))
                && (user.permissions().contains("dataset:read") || user.permissions().contains("trace:read"))) return;
        throw BusinessException.forbidden("无权访问该业务模块");
    }

    private void requireWrite() {
        UserPrincipal user = CurrentUser.require();
        if (!user.admin() && !user.permissions().contains("trace:write"))
            throw BusinessException.forbidden("无权维护业务对象");
    }

    private ModuleDefinition requireWritable(String module) {
        ModuleDefinition definition = MODULES.get(module);
        if (definition == null) throw BusinessException.badRequest("该模块不支持业务对象维护");
        return definition;
    }

    private String normalizeModule(String module) {
        return module == null ? "" : module.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> normalizedProperties(BusinessModuleController.RecordBody body) {
        Map<String, Object> properties = new LinkedHashMap<>(body.properties());
        if (body.status() != null && !body.status().isBlank()) properties.put("status", body.status().trim());
        return properties;
    }

    private String normalizedCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizedSource(String source) {
        return source == null || source.isBlank() ? "MANUAL" : source.trim().toUpperCase(Locale.ROOT);
    }

    private void validateCode(String code) {
        if (!normalizedCode(code).matches("^[A-Z0-9][A-Z0-9._/-]{1,99}$"))
            throw BusinessException.badRequest("业务编码须为2至100位大写字母、数字、点、斜杠、下划线或连字符");
    }

    private String status(Map<String, Object> properties) {
        for (String key : List.of("status", "stage", "state")) {
            Object value = properties.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return "有效";
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "MATERIAL", "MATERIAL_BATCH", "RAW_MATERIAL" -> "材料";
            case "PROCESS", "PROCESS_BATCH", "PROCESS_ROUTE", "OPERATION" -> "工艺";
            case "PRODUCT", "PRODUCT_BATCH", "PART" -> "产品";
            case "PERFORMANCE", "TEST_RESULT", "QUALITY_INSPECTION" -> "性能 / 检验";
            case "RND_PROJECT", "PROJECT" -> "研发项目";
            case "EXPERIMENT", "LAB_EXPERIMENT" -> "实验";
            case "PROCESS_EXPERIMENT" -> "工艺实验";
            case "SIMULATION", "SIMULATION_TASK" -> "仿真任务";
            default -> type;
        };
    }

    private String moduleLabel(String module) {
        return switch (module) {
            case "materials" -> "材料";
            case "processes" -> "工艺";
            case "products" -> "产品";
            case "performance" -> "性能";
            case "rnd-projects" -> "研发项目";
            case "experiments" -> "实验";
            case "process-experiments" -> "工艺实验";
            case "simulations" -> "仿真";
            default -> "业务";
        };
    }

    private boolean failure(String operation) {
        String value = operation == null ? "" : operation.toUpperCase(Locale.ROOT);
        return value.contains("FAIL") || value.contains("ERROR") || value.contains("REJECT");
    }

    private String time(Object value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String number(long value) {
        return java.text.NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private record ModuleDefinition(String writeType, List<String> types) {
    }

    private record Aggregate(long total, long complete, long issues, long recent) {
    }
}
