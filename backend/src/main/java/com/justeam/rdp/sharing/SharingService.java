package com.justeam.rdp.sharing;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SharingService {
    private static final Set<String> RESOURCE_TYPES=Set.of("DATA_SCOPE","DATASET");
    private static final Set<String> GRANTEE_TYPES=Set.of("USER","ROLE","DEPARTMENT");
    private static final Set<String> OPERATIONS=Set.of("READ","EXPORT","DOWNLOAD");
    private final JdbcClient jdbc;private final AuditService audit;private final TransactionTemplate transactions;
    public SharingService(JdbcClient jdbc,AuditService audit,TransactionTemplate transactions){this.jdbc=jdbc;this.audit=audit;this.transactions=transactions;}

    public PageResponse<Map<String,Object>> list(String status,String resourceType,int pageNum,int pageSize){
        expireDueNow();int page=Math.max(1,pageNum),size=Math.max(1,Math.min(200,pageSize));String normalizedStatus=blank(status),normalizedResource=blank(resourceType);
        if(normalizedStatus!=null&&!Set.of("ACTIVE","REVOKED","EXPIRED").contains(normalizedStatus))throw BusinessException.badRequest("共享状态不正确");
        if(normalizedResource!=null&&!RESOURCE_TYPES.contains(normalizedResource))throw BusinessException.badRequest("共享资源类型不正确");
        String where=" FROM sys_share_rule s WHERE (CAST(:status AS text) IS NULL OR s.status=CAST(:status AS text)) AND (CAST(:resource AS text) IS NULL OR s.resource_type=CAST(:resource AS text))";
        long total=jdbc.sql("SELECT count(*)"+where).param("status",normalizedStatus).param("resource",normalizedResource).query(Long.class).single();
        List<Map<String,Object>> rows=jdbc.sql("SELECT s.*,cu.username creator_username,ru.username revoked_username FROM sys_share_rule s LEFT JOIN sys_user cu ON cu.id=s.created_by LEFT JOIN sys_user ru ON ru.id=s.revoked_by WHERE (CAST(:status AS text) IS NULL OR s.status=CAST(:status AS text)) AND (CAST(:resource AS text) IS NULL OR s.resource_type=CAST(:resource AS text)) ORDER BY s.created_time DESC,s.id DESC LIMIT :limit OFFSET :offset")
                .param("status",normalizedStatus).param("resource",normalizedResource).param("limit",size).param("offset",(page-1)*size)
                .query(this::row).list();enrichNames(rows);return PageResponse.of(total,page,size,rows);
    }

    public List<Map<String,Object>> mine(){expireDueNow();UserPrincipal user=CurrentUser.require();List<Map<String,Object>> rows=jdbc.sql("""
            SELECT s.*,cu.username creator_username,ru.username revoked_username
            FROM sys_share_rule s
            LEFT JOIN sys_user cu ON cu.id=s.created_by LEFT JOIN sys_user ru ON ru.id=s.revoked_by
            WHERE s.status='ACTIVE' AND s.valid_from<=now() AND (s.valid_to IS NULL OR s.valid_to>now())
              AND ((s.grantee_type='USER' AND s.grantee_id=:user)
                OR (s.grantee_type='ROLE' AND EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0 WHERE ur.user_id=:user AND ur.role_id=s.grantee_id))
                OR (s.grantee_type='DEPARTMENT' AND EXISTS(SELECT 1 FROM sys_user u JOIN sys_department d ON d.id=u.department_id AND d.status=1 AND d.deleted=0 WHERE u.id=:user AND u.department_id=s.grantee_id)))
            ORDER BY s.valid_to NULLS LAST,s.id
            """).param("user",user.id()).query(this::row).list();enrichNames(rows);return rows;}

    public Map<String,Object> options(){Map<String,Object> result=new LinkedHashMap<>();result.put("users",jdbc.sql("SELECT id,username,real_name name FROM sys_user WHERE status=1 AND deleted=0 ORDER BY real_name,username").query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"username",rs.getString("username"),"name",rs.getString("name"))).list());result.put("roles",jdbc.sql("SELECT id,role_code code,role_name name FROM sys_role WHERE status=1 AND deleted=0 ORDER BY sort_order,id").query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("code"),"name",rs.getString("name"))).list());result.put("departments",jdbc.sql("SELECT id,dept_code code,dept_name name FROM sys_department WHERE status=1 AND deleted=0 ORDER BY id").query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("code"),"name",rs.getString("name"))).list());result.put("scopes",jdbc.sql("SELECT id,scope_code code,scope_name name,scope_type type FROM sys_data_scope WHERE active=TRUE ORDER BY id").query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("code"),"name",rs.getString("name"),"type",rs.getString("type"))).list());result.put("datasets",jdbc.sql("SELECT d.id,d.name,d.data_scope_id FROM data_dataset d JOIN sys_data_scope s ON s.id=d.data_scope_id AND s.active=TRUE WHERE d.deleted=0 ORDER BY d.name,d.id").query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"name",rs.getString("name"),"dataScopeId",rs.getLong("data_scope_id"))).list());return result;}

    @Transactional public long create(String resourceType,long resourceId,String granteeType,long granteeId,List<String> rawOperations,Instant validFrom,Instant validTo,String reason){
        expireDueLocked();String resource=normalize(resourceType,RESOURCE_TYPES,"共享资源类型不正确"),grantee=normalize(granteeType,GRANTEE_TYPES,"共享目标类型不正确");List<String> operations=normalizeOperations(rawOperations);
        Instant databaseNow=jdbc.sql("SELECT current_timestamp").query(OffsetDateTime.class).single().toInstant();
        Instant start=validFrom==null?databaseNow:validFrom;if(validTo!=null&&!validTo.isAfter(start))throw BusinessException.badRequest("共享结束时间必须晚于开始时间");
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(99172020)) x").query(Long.class).single();
        for(long key:List.of(4203L,4204L,4205L,4207L))jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(:key)) locked").param("key",key).query(Long.class).single();
        validateResource(resource,resourceId);validateGrantee(grantee,granteeId);if(reason==null||reason.isBlank())throw BusinessException.badRequest("共享原因不能为空");
        long duplicate=jdbc.sql("SELECT count(*) FROM sys_share_rule WHERE resource_type=:resource AND resource_id=:resourceId AND grantee_type=:grantee AND grantee_id=:granteeId AND status='ACTIVE'")
                .param("resource",resource).param("resourceId",resourceId).param("grantee",grantee).param("granteeId",granteeId).query(Long.class).single();
        if(duplicate>0)throw new BusinessException(409,"同一资源与目标已有生效共享，请先撤销旧规则");
        long id=jdbc.sql("""
                INSERT INTO sys_share_rule(resource_type,resource_id,grantee_type,grantee_id,operations,valid_from,valid_to,reason,created_by)
                VALUES (:resource,:resourceId,:grantee,:granteeId,:operations,:validFrom,:validTo,:reason,:actor) RETURNING id
                """).param("resource",resource).param("resourceId",resourceId).param("grantee",grantee).param("granteeId",granteeId)
                .param("operations",operations.toArray(String[]::new)).param("validFrom",java.sql.Timestamp.from(start))
                .param("validTo",validTo==null?null:java.sql.Timestamp.from(validTo)).param("reason",reason.trim()).param("actor",CurrentUser.require().id()).query(Long.class).single();
        audit.record("SHARE_CREATE","SHARING","创建选择性共享规则",Map.of("shareRuleId",id,"resourceType",resource,"resourceId",resourceId,"granteeType",grantee,"granteeId",granteeId,"operations",operations,"reason",reason.trim()));return id;
    }

    @Transactional public void revoke(long id,int expectedVersion,String reason){
        if(reason==null||reason.isBlank())throw BusinessException.badRequest("撤销原因不能为空");Map<String,Object> before=jdbc.sql("SELECT * FROM sys_share_rule WHERE id=:id FOR UPDATE").param("id",id).query(this::rowRaw).optional().orElseThrow(()->BusinessException.notFound("共享规则不存在"));
        if(!"ACTIVE".equals(before.get("status")))throw BusinessException.badRequest("只有生效规则可以撤销");if(((Number)before.get("version")).intValue()!=expectedVersion)throw new BusinessException(409,"共享规则版本已变化，请刷新后重试");
        jdbc.sql("UPDATE sys_share_rule SET status='REVOKED',revoked_by=:actor,revoked_time=now(),revoke_reason=:reason,version=version+1 WHERE id=:id AND status='ACTIVE' AND version=:version")
                .param("actor",CurrentUser.require().id()).param("reason",reason.trim()).param("id",id).param("version",expectedVersion).update();
        Map<String,Object> after=new LinkedHashMap<>(before);after.put("status","REVOKED");after.put("version",expectedVersion+1);after.put("revokeReason",reason.trim());
        audit.record("SHARE_REVOKE","SHARING","撤销选择性共享规则",Map.of("shareRuleId",id,"before",before,"after",after,"reason",reason.trim()));
    }

    public Set<Long> sharedResourceIds(String resourceType,String operation){UserPrincipal user=CurrentUser.require();if(user.admin())return Set.of();String resource=normalize(resourceType,RESOURCE_TYPES,"共享资源类型不正确"),op=normalize(operation,OPERATIONS,"共享操作不正确");return new LinkedHashSet<>(jdbc.sql(effectiveSql("SELECT DISTINCT s.resource_id",resource,op)).param("user",user.id()).query(Long.class).list());}
    public boolean hasResourceAccess(String resourceType,long resourceId,String operation){UserPrincipal user=CurrentUser.require();if(user.admin())return true;String resource=normalize(resourceType,RESOURCE_TYPES,"共享资源类型不正确"),op=normalize(operation,OPERATIONS,"共享操作不正确");return jdbc.sql(effectiveSql("SELECT count(*)",resource,op)+" AND s.resource_id=:resourceId").param("user",user.id()).param("resourceId",resourceId).query(Long.class).single()>0;}
    public boolean hasScopeAccess(long scopeId,String operation){return hasResourceAccess("DATA_SCOPE",scopeId,operation);}

    @Scheduled(initialDelayString="${rdp.sharing.expire-initial-delay-ms:30000}",fixedDelayString="${rdp.sharing.expire-delay-ms:60000}")
    public void expireDue(){expireDueNow();}
    private void expireDueNow(){transactions.executeWithoutResult(status->expireDueLocked());}
    private void expireDueLocked(){while(true){List<Long> ids=jdbc.sql("SELECT id FROM sys_share_rule WHERE status='ACTIVE' AND valid_to IS NOT NULL AND valid_to<=now() ORDER BY id FOR UPDATE SKIP LOCKED LIMIT 500").query(Long.class).list();if(ids.isEmpty())return;jdbc.sql("UPDATE sys_share_rule SET status='EXPIRED',version=version+1 WHERE id IN (:ids) AND status='ACTIVE'").param("ids",ids).update();for(Long id:ids)audit.recordAs(null,"system","SHARE_EXPIRE","SHARING","选择性共享规则自动过期",Map.of("shareRuleId",id));}}

    private String effectiveSql(String select,String resource,String operation){String liveResource="DATA_SCOPE".equals(resource)
            ?"EXISTS(SELECT 1 FROM sys_data_scope ds WHERE ds.id=s.resource_id AND ds.active=TRUE)"
            :"EXISTS(SELECT 1 FROM data_dataset dd JOIN sys_data_scope ds ON ds.id=dd.data_scope_id AND ds.active=TRUE WHERE dd.id=s.resource_id AND dd.deleted=0)";
        return select+" FROM sys_share_rule s WHERE s.resource_type='"+resource+"' AND s.status='ACTIVE' AND s.valid_from<=now() AND (s.valid_to IS NULL OR s.valid_to>now()) AND "+liveResource+" AND '"+operation+"'=ANY(s.operations) AND ((s.grantee_type='USER' AND s.grantee_id=:user) OR (s.grantee_type='ROLE' AND EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0 WHERE ur.user_id=:user AND ur.role_id=s.grantee_id)) OR (s.grantee_type='DEPARTMENT' AND EXISTS(SELECT 1 FROM sys_user u JOIN sys_department d ON d.id=u.department_id AND d.status=1 AND d.deleted=0 WHERE u.id=:user AND u.department_id=s.grantee_id)))";}
    private List<String> normalizeOperations(List<String> raw){if(raw==null||raw.isEmpty())throw BusinessException.badRequest("共享操作不能为空");LinkedHashSet<String> values=new LinkedHashSet<>();for(String value:raw)values.add(normalize(value,OPERATIONS,"共享操作不正确"));if(!values.contains("READ"))throw BusinessException.badRequest("共享操作必须包含READ");return new ArrayList<>(values);}
    private void validateResource(String type,long id){long count=switch(type){case "DATA_SCOPE"->jdbc.sql("SELECT count(*) FROM sys_data_scope WHERE id=:id AND active=TRUE").param("id",id).query(Long.class).single();case "DATASET"->jdbc.sql("SELECT count(*) FROM data_dataset d JOIN sys_data_scope s ON s.id=d.data_scope_id AND s.active=TRUE WHERE d.id=:id AND d.deleted=0").param("id",id).query(Long.class).single();default->0;};if(count==0)throw BusinessException.notFound("共享资源不存在或已停用");}
    private void validateGrantee(String type,long id){long count=switch(type){case "USER"->jdbc.sql("SELECT count(*) FROM sys_user WHERE id=:id AND status=1 AND deleted=0").param("id",id).query(Long.class).single();case "ROLE"->jdbc.sql("SELECT count(*) FROM sys_role WHERE id=:id AND status=1 AND deleted=0").param("id",id).query(Long.class).single();case "DEPARTMENT"->jdbc.sql("SELECT count(*) FROM sys_department WHERE id=:id AND status=1 AND deleted=0").param("id",id).query(Long.class).single();default->0;};if(count==0)throw BusinessException.notFound("共享目标不存在或已停用");}
    private void enrichNames(List<Map<String,Object>> rows){for(Map<String,Object> row:rows){String type=String.valueOf(row.get("resourceType"));long resource=((Number)row.get("resourceId")).longValue();String resourceName="DATA_SCOPE".equals(type)?jdbc.sql("SELECT scope_name FROM sys_data_scope WHERE id=:id").param("id",resource).query(String.class).optional().orElse("已删除数据域"):jdbc.sql("SELECT name FROM data_dataset WHERE id=:id").param("id",resource).query(String.class).optional().orElse("已删除数据集");String granteeType=String.valueOf(row.get("granteeType"));long grantee=((Number)row.get("granteeId")).longValue();String granteeName=switch(granteeType){case "USER"->jdbc.sql("SELECT real_name FROM sys_user WHERE id=:id").param("id",grantee).query(String.class).optional().orElse("已删除用户");case "ROLE"->jdbc.sql("SELECT role_name FROM sys_role WHERE id=:id").param("id",grantee).query(String.class).optional().orElse("已删除角色");default->jdbc.sql("SELECT dept_name FROM sys_department WHERE id=:id").param("id",grantee).query(String.class).optional().orElse("已删除部门");};row.put("resourceName",resourceName);row.put("granteeName",granteeName);}}
    private Map<String,Object> row(ResultSet rs,int n)throws SQLException{Map<String,Object> value=rowRaw(rs,n);value.put("creatorUsername",rs.getString("creator_username"));value.put("revokedUsername",rs.getString("revoked_username"));return value;}
    private Map<String,Object> rowRaw(ResultSet rs,int n)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("resourceType",rs.getString("resource_type"));v.put("resourceId",rs.getLong("resource_id"));v.put("granteeType",rs.getString("grantee_type"));v.put("granteeId",rs.getLong("grantee_id"));v.put("operations",strings(rs.getArray("operations")));v.put("validFrom",rs.getObject("valid_from"));v.put("validTo",rs.getObject("valid_to"));v.put("status",rs.getString("status"));v.put("reason",rs.getString("reason"));v.put("createdBy",rs.getLong("created_by"));v.put("createdTime",rs.getObject("created_time"));v.put("revokedBy",rs.getObject("revoked_by"));v.put("revokedTime",rs.getObject("revoked_time"));v.put("revokeReason",rs.getString("revoke_reason"));v.put("version",rs.getInt("version"));return v;}
    private List<String> strings(Array array)throws SQLException{return array==null?List.of():Arrays.stream((Object[])array.getArray()).map(Object::toString).toList();}
    private String normalize(String raw,Set<String> allowed,String message){String value=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);if(!allowed.contains(value))throw BusinessException.badRequest(message);return value;}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim().toUpperCase(Locale.ROOT);}
}
