package com.justeam.rdp.trace;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.file.FileAssetService;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.DataScopeService;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TraceService {
    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final DataScopeService scopes;
    private final AuditService audit;
    private final FileAssetService files;

    public TraceService(JdbcClient jdbc, JsonSupport json, DataScopeService scopes, AuditService audit,
                        FileAssetService files) {
        this.jdbc = jdbc; this.json = json; this.scopes = scopes; this.audit = audit; this.files = files;
    }

    public List<Map<String, Object>> entities(String keyword, String type) {
        UserPrincipal user = CurrentUser.require();
        String scope = user.admin() ? "TRUE" : user.dataScopes().isEmpty() ? "FALSE" : "data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec spec = jdbc.sql("""
                SELECT * FROM trace_entity WHERE deleted=0
                AND (CAST(:keyword AS text) IS NULL OR lower(entity_code) LIKE lower(concat('%',CAST(:keyword AS text),'%')) OR lower(entity_name) LIKE lower(concat('%',CAST(:keyword AS text),'%')))
                AND (CAST(:type AS text) IS NULL OR entity_type=CAST(:type AS text))
                """ + " AND " + scope + " ORDER BY created_time DESC LIMIT 200")
                .param("keyword", blank(keyword)).param("type", blank(type));
        if (!user.admin() && !user.dataScopes().isEmpty()) spec = spec.param("scopes", user.dataScopes());
        return spec.query(this::entityRow).list();
    }

    @Transactional
    public long create(EntityBody body) {
        scopes.require(body.dataScopeId());
        UserPrincipal user = CurrentUser.require();
        Long id = jdbc.sql("""
                INSERT INTO trace_entity(entity_type,entity_code,entity_name,properties,data_scope_id,source_system,source_record_id,created_by)
                VALUES (:type,:code,:name,CAST(:properties AS jsonb),:scope,:source,:sourceId,:userId) RETURNING id
                """).param("type", body.entityType()).param("code", body.entityCode()).param("name", body.entityName())
                .param("properties", json.write(body.properties())).param("scope", body.dataScopeId())
                .param("source", body.sourceSystem() == null ? "MANUAL" : body.sourceSystem())
                .param("sourceId", body.sourceRecordId()).param("userId", user.id()).query(Long.class).single();
        audit.record("CREATE", "TRACE", "创建追溯实体", Map.of("entityId", id, "type", body.entityType(), "code", body.entityCode()));
        return id;
    }

    @Transactional
    public long relate(RelationBody body) {
        if (body.fromEntityId() == body.toEntityId()) throw BusinessException.badRequest("追溯关系不能指向自身");
        if (body.relationType() == null || !body.relationType().matches("^[A-Z][A-Z0-9_]{1,59}$"))
            throw BusinessException.badRequest("关系类型须为大写字母、数字或下划线");
        Map<String, Object> from = entity(body.fromEntityId());
        Map<String, Object> to = entity(body.toEntityId());
        scopes.require(((Number) from.get("dataScopeId")).longValue());
        scopes.require(((Number) to.get("dataScopeId")).longValue());
        if (!CurrentUser.require().admin() && !from.get("dataScopeId").equals(to.get("dataScopeId")))
            throw BusinessException.forbidden("跨数据域关系仅允许管理员在共享规则确认后创建");
        Long id = jdbc.sql("""
                INSERT INTO trace_relation(from_entity_id,to_entity_id,relation_type,properties,effective_time,created_by)
                VALUES (:fromId,:toId,:type,CAST(:properties AS jsonb),COALESCE(:effectiveTime,now()),:userId) RETURNING id
                """).param("fromId", body.fromEntityId()).param("toId", body.toEntityId()).param("type", body.relationType())
                .param("properties", json.write(body.properties())).param("effectiveTime", body.effectiveTime())
                .param("userId", CurrentUser.require().id()).query(Long.class).single();
        audit.record("CREATE_RELATION", "TRACE", "创建追溯关系", Map.of("relationId", id, "from", body.fromEntityId(), "to", body.toEntityId(), "type", body.relationType()));
        return id;
    }

    public Map<String, Object> graph(long rootId, String direction, int maxDepth) {
        Map<String, Object> root = entity(rootId);
        scopes.requireRead(((Number) root.get("dataScopeId")).longValue());
        if (maxDepth < 1 || maxDepth > 10) throw BusinessException.badRequest("追溯深度必须在1到10之间");
        int depth = maxDepth;
        String normalizedDirection = direction == null ? "both" : direction.toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("forward", "reverse", "both").contains(normalizedDirection))
            throw BusinessException.badRequest("direction仅支持forward、reverse或both");
        String edgeCondition = switch (normalizedDirection) {
            case "forward" -> "r.from_entity_id = w.node_id";
            case "reverse" -> "r.to_entity_id = w.node_id";
            case "both" -> "(r.from_entity_id = w.node_id OR r.to_entity_id = w.node_id)";
            default -> throw new IllegalStateException();
        };
        String nextNode = "forward".equals(normalizedDirection) ? "r.to_entity_id" : "reverse".equals(normalizedDirection)
                ? "r.from_entity_id" : "CASE WHEN r.from_entity_id=w.node_id THEN r.to_entity_id ELSE r.from_entity_id END";
        UserPrincipal user = CurrentUser.require();
        String visibleScope = user.admin() ? "TRUE" : "n.data_scope_id IN (:scopes)";
        String graphSql = """
                WITH RECURSIVE walk(node_id,depth) AS (
                  SELECT :rootId::bigint,0
                  UNION
                  SELECT 
                """ + nextNode + ",w.depth+1 FROM walk w JOIN trace_relation r ON " + edgeCondition
                + " JOIN trace_entity n ON n.id=" + nextNode + " AND n.deleted=0 AND " + visibleScope
                + " WHERE w.depth<:depth), traversed AS (SELECT DISTINCT r.id,r.from_entity_id,r.to_entity_id,"
                + "r.relation_type,r.properties,r.effective_time FROM walk w JOIN trace_relation r ON " + edgeCondition
                + " JOIN trace_entity n ON n.id=" + nextNode + " AND n.deleted=0 AND " + visibleScope
                + " WHERE w.depth<:depth) SELECT * FROM traversed LIMIT 1001";
        JdbcClient.StatementSpec query = jdbc.sql(graphSql).param("rootId", rootId).param("depth", depth);
        if (!user.admin()) query = query.param("scopes", user.dataScopes());
        List<Map<String, Object>> edges = query.query((rs, row) -> {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", rs.getLong("id")); edge.put("from", rs.getLong("from_entity_id"));
            edge.put("to", rs.getLong("to_entity_id")); edge.put("type", rs.getString("relation_type"));
            edge.put("properties", json.map(rs.getString("properties"))); edge.put("effectiveTime", rs.getObject("effective_time"));
            return edge;
        }).list();
        if (edges.size() > 1000) throw BusinessException.badRequest("追溯图超过1000条关系，请缩小深度或增加筛选条件");
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>(); ids.add(rootId);
        edges.forEach(e -> { ids.add(((Number)e.get("from")).longValue()); ids.add(((Number)e.get("to")).longValue()); });
        if (ids.size() > 1000) throw BusinessException.badRequest("追溯图超过1000个节点，请缩小深度");
        List<Map<String, Object>> nodes = jdbc.sql("SELECT * FROM trace_entity WHERE deleted=0 AND id IN (:ids)")
                .param("ids", ids).query(this::entityRow).list();
        return Map.of("rootId", rootId, "direction", normalizedDirection, "nodes", nodes, "edges", edges);
    }

    /**
     * Returns only evidence that is anchored to a visible trace entity. Relation audit rows are included only when
     * the opposite entity is also visible to the current user, preventing a relation from becoming a scope side
     * channel. Global audit details are deliberately reduced to the fields needed to verify the trace evidence.
     */
    public Map<String, Object> evidence(long entityId) {
        Map<String, Object> entity = entity(entityId);
        scopes.requireRead(((Number) entity.get("dataScopeId")).longValue());
        UserPrincipal user = CurrentUser.require();
        boolean attachmentAccess = user.admin() || user.permissions().contains("file:read");
        List<Map<String, Object>> attachments = attachmentAccess
                ? files.list("TRACE_ENTITY", Long.toString(entityId)) : List.of();
        String entityRef = Long.toString(entityId);
        String oppositeVisible = user.admin() ? "TRUE" : user.dataScopes().isEmpty()
                ? "FALSE" : "other.data_scope_id IN (:scopes)";
        String predicate = """
                ((a.module='TRACE' AND (
                    a.details->>'entityId'=:entityRef OR
                    ((a.details->>'from'=:entityRef OR a.details->>'to'=:entityRef) AND EXISTS (
                        SELECT 1 FROM trace_entity other
                        WHERE other.deleted=0
                          AND other.id::text=CASE WHEN a.details->>'from'=:entityRef
                              THEN a.details->>'to' ELSE a.details->>'from' END
                          AND (%s)
                    ))
                )) OR (a.module='FILE' AND a.details->>'businessType'='TRACE_ENTITY'
                    AND a.details->>'businessRef'=:entityRef))
                """.formatted(oppositeVisible);
        JdbcClient.StatementSpec countSpec = jdbc.sql("SELECT count(*) FROM sys_audit_log a WHERE " + predicate)
                .param("entityRef", entityRef);
        JdbcClient.StatementSpec auditSpec = jdbc.sql("""
                SELECT a.id,a.username,a.operation_type,a.module,a.description,a.details::text AS details,
                       a.audit_key_id,a.record_digest,a.created_time
                FROM sys_audit_log a WHERE
                """ + predicate + " ORDER BY a.created_time DESC,a.id DESC LIMIT 100")
                .param("entityRef", entityRef);
        if (!user.admin() && !user.dataScopes().isEmpty()) {
            countSpec = countSpec.param("scopes", user.dataScopes());
            auditSpec = auditSpec.param("scopes", user.dataScopes());
        }
        long auditTotal = countSpec.query(Long.class).single();
        List<Map<String, Object>> auditEvents = auditSpec.query((rs, row) -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", rs.getLong("id")); value.put("username", rs.getString("username"));
            value.put("operation", rs.getString("operation_type")); value.put("module", rs.getString("module"));
            value.put("description", rs.getString("description"));
            Map<String, Object> details = json.map(rs.getString("details"));
            Map<String, Object> safeDetails = new LinkedHashMap<>();
            for (String key : List.of("entityId", "relationId", "from", "to", "type", "code",
                    "fileId", "name", "sha256", "sizeBytes", "businessType", "businessRef")) {
                if (details.containsKey(key)) safeDetails.put(key, details.get(key));
            }
            value.put("details", safeDetails); value.put("auditKeyId", rs.getString("audit_key_id"));
            value.put("recordDigest", rs.getString("record_digest")); value.put("createdTime", rs.getObject("created_time"));
            return value;
        }).list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entity", entity); result.put("attachmentAccess", attachmentAccess);
        result.put("attachments", attachments); result.put("attachmentTotal", attachments.size());
        result.put("auditEvents", auditEvents); result.put("auditTotal", auditTotal);
        result.put("auditLimited", auditTotal > auditEvents.size());
        return result;
    }

    private Map<String, Object> entity(long id) {
        return jdbc.sql("SELECT * FROM trace_entity WHERE id=:id AND deleted=0").param("id", id)
                .query(this::entityRow).optional().orElseThrow(() -> BusinessException.notFound("追溯实体不存在"));
    }

    private Map<String, Object> entityRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("entityType", rs.getString("entity_type"));
        value.put("entityCode", rs.getString("entity_code")); value.put("entityName", rs.getString("entity_name"));
        value.put("properties", json.map(rs.getString("properties"))); value.put("dataScopeId", rs.getLong("data_scope_id"));
        value.put("sourceSystem", rs.getString("source_system")); value.put("sourceRecordId", rs.getString("source_record_id"));
        value.put("version", rs.getInt("version")); value.put("createdTime", rs.getObject("created_time")); return value;
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public record EntityBody(String entityType, String entityCode, String entityName, Map<String,Object> properties,
                             long dataScopeId, String sourceSystem, String sourceRecordId) {}
    public record RelationBody(long fromEntityId, long toEntityId, String relationType, Map<String,Object> properties,
                               java.time.Instant effectiveTime) {}
}
