package com.justeam.rdp.dashboard;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.sharing.SharingService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

@Service
public class DashboardService {
    private final JdbcClient jdbc;
    private final AuditService audit;
    private final SharingService sharing;
    public DashboardService(JdbcClient jdbc,AuditService audit,SharingService sharing) { this.jdbc = jdbc;this.audit=audit;this.sharing=sharing; }

    public Map<String, Object> overview() {
        UserPrincipal user = CurrentUser.require();
        Map<String, Object> result = new LinkedHashMap<>();
        Long templateCount=can(user,"template:read")?templateCount(user,"template"):null;
        Long fragmentCount=can(user,"template:read")?templateCount(user,"fragment"):null;
        Long datasetCount=can(user,"dataset:read")?datasetMetric(user,"count(*)","READ"):null;
        Long recordCount=can(user,"dataset:read")?recordMetric(user,"ALL","READ"):null;
        Long publishedRecordCount=can(user,"dataset:read")?recordMetric(user,"PUBLISHED","READ"):null;
        Long publishingRecordCount=can(user,"dataset:read")?recordMetric(user,"PUBLISHING","READ"):null;
        result.put("templateCount",templateCount);result.put("fragmentCount",fragmentCount);
        result.put("datasetCount",datasetCount);result.put("recordCount",recordCount);
        result.put("publishedRecordCount",publishedRecordCount);result.put("publishingRecordCount",publishingRecordCount);
        // 兼容已有客户端字段；新页面和验收均使用上方六个明确口径字段。
        result.put("templates",templateCount==null||fragmentCount==null?null:templateCount+fragmentCount);
        result.put("datasets",datasetCount);result.put("records",recordCount);
        result.put("traceEntities",can(user,"trace:read")?count("trace_entity", "deleted=0", user):null);
        boolean deviceVisible=can(user,"device:read");
        result.put("devices",deviceVisible?count("device", "TRUE", user):null);
        result.put("onlineDevices",deviceVisible?count("device", "status='ONLINE'", user):null);
        result.put("auditEventsToday",can(user,"audit:read")?jdbc.sql("SELECT count(*) FROM sys_audit_log WHERE created_time>=date_trunc('day',now())")
                .query(Long.class).single():null);
        result.put("deviceTrend",deviceVisible?deviceTrend(user):List.of());
        return result;
    }

    private boolean can(UserPrincipal user,String permission){return user.admin()||user.permissions().contains(permission);}

    public Map<String,Object> decisionReport(){return decisionReport("READ");}
    private Map<String,Object> decisionReport(String operation){long started=System.nanoTime();UserPrincipal user=CurrentUser.require();Map<String,Object> report=new LinkedHashMap<>();SetView access=access(user,operation);
        report.put("generatedTime",Instant.now().toString());report.put("scopeCount",user.admin()?jdbc.sql("SELECT count(*) FROM sys_data_scope WHERE active=TRUE").query(Long.class).single():access.scopes().size());
        if(can(user,"dataset:read")){report.put("datasetByCategory",datasetGroups(user,access));report.put("recordLifecycle",recordLifecycle(user,access));report.put("publicationTrend",publicationTrend(user,access));}
        else{report.put("datasetByCategory",List.of());report.put("recordLifecycle",List.of());report.put("publicationTrend",List.of());}
        if(can(user,"device:read")){report.put("deviceStatus",deviceStatus(user,access));report.put("measurementTrend",deviceTrend(user,access));}else{report.put("deviceStatus",List.of());report.put("measurementTrend",List.of());}
        report.put("traceByType",can(user,"trace:read")?traceGroups(user,access):List.of());
        Map<String,Long> lifecycle=new java.util.HashMap<>();for(Map<String,Object> row:list(report,"recordLifecycle"))lifecycle.put(String.valueOf(row.get("name")),((Number)row.get("value")).longValue());
        long published=lifecycle.getOrDefault("PUBLISHED",0L),total=lifecycle.values().stream().mapToLong(Long::longValue).sum();
        Map<String,Long> devices=new java.util.HashMap<>();for(Map<String,Object> row:list(report,"deviceStatus"))devices.put(String.valueOf(row.get("name")),((Number)row.get("value")).longValue());
        long allDevices=devices.values().stream().mapToLong(Long::longValue).sum();Map<String,Object> kpis=new LinkedHashMap<>();kpis.put("publicationRate",total==0?0:Math.round(published*1000.0/total)/10.0);kpis.put("onlineRate",allDevices==0?0:Math.round(devices.getOrDefault("ONLINE",0L)*1000.0/allDevices)/10.0);kpis.put("pendingReview",lifecycle.getOrDefault("PENDING_REVIEW",0L));report.put("kpis",kpis);
        audit.record("REPORT","DASHBOARD","生成授权范围汇总报表",Map.of("elapsedMs",(System.nanoTime()-started)/1_000_000,"sections",List.of("datasetByCategory","recordLifecycle","publicationTrend","deviceStatus","measurementTrend","traceByType")));
        return report;}

    public byte[] reportCsv(){Map<String,Object> report=decisionReport("EXPORT");StringBuilder csv=new StringBuilder("\uFEFFsection,name,value\r\n");
        for(String section:List.of("datasetByCategory","recordLifecycle","publicationTrend","deviceStatus","measurementTrend","traceByType"))for(Map<String,Object> row:list(report,section)){Object name=row.containsKey("name")?row.get("name"):row.get("day");csv.append(csv(section)).append(',').append(csv(String.valueOf(name))).append(',').append(row.get("value")).append("\r\n");}
        csv.append(csv("KPI")).append(',').append(csv("scopeCount")).append(',').append(report.get("scopeCount")).append("\r\n");Object rawKpis=report.get("kpis");if(rawKpis instanceof Map<?,?> kpis)for(var entry:kpis.entrySet())csv.append(csv("KPI")).append(',').append(csv(String.valueOf(entry.getKey()))).append(',').append(entry.getValue()).append("\r\n");
        audit.record("EXPORT","DASHBOARD","导出授权范围决策报表",Map.of("format","CSV","sectionCount",7));return csv.toString().getBytes(StandardCharsets.UTF_8);}

    private List<Map<String,Object>> datasetGroups(UserPrincipal user,SetView access){return grouped("SELECT coalesce(nullif(trim(d.category),''),'未分类') name,count(*) value FROM data_dataset d WHERE d.deleted=0 AND "+datasetVisible(user,access)+" GROUP BY coalesce(nullif(trim(d.category),''),'未分类') ORDER BY value DESC,name",user,access);}
    private List<Map<String,Object>> recordLifecycle(UserPrincipal user,SetView access){String visible=user.admin()?"TRUE":"(w.owner_id=:user OR w.status='PUBLISHED'"+(can(user,"dataset:audit")&&!user.assignedScopes().isEmpty()?" OR d.data_scope_id IN (:assignedScopes)":"")+")";return grouped("SELECT w.status name,count(*) value FROM dataset_record_workflow w JOIN data_dataset d ON d.id=w.dataset_id WHERE d.deleted=0 AND w.status<>'DELETED' AND "+datasetVisible(user,access)+" AND "+visible+" GROUP BY w.status ORDER BY w.status",user,access);}
    private List<Map<String,Object>> publicationTrend(UserPrincipal user,SetView access){return grouped("SELECT to_char(date_trunc('day',w.published_time),'YYYY-MM-DD') name,count(*) value FROM dataset_record_workflow w JOIN data_dataset d ON d.id=w.dataset_id WHERE d.deleted=0 AND w.status='PUBLISHED' AND w.published_time>=now()-interval '30 days' AND "+datasetVisible(user,access)+" GROUP BY date_trunc('day',w.published_time) ORDER BY date_trunc('day',w.published_time)",user,access);}
    private List<Map<String,Object>> deviceStatus(UserPrincipal user,SetView access){String scope=user.admin()?"TRUE":access.scopes().isEmpty()?"FALSE":"d.data_scope_id IN (:scopes)";return grouped("SELECT d.status name,count(*) value FROM device d WHERE "+scope+" GROUP BY d.status ORDER BY d.status",user,access);}
    private List<Map<String,Object>> traceGroups(UserPrincipal user,SetView access){String scope=user.admin()?"TRUE":access.scopes().isEmpty()?"FALSE":"e.data_scope_id IN (:scopes)";return grouped("SELECT e.entity_type name,count(*) value FROM trace_entity e WHERE e.deleted=0 AND "+scope+" GROUP BY e.entity_type ORDER BY value DESC,e.entity_type",user,access);}
    private List<Map<String,Object>> grouped(String sql,UserPrincipal user,SetView access){JdbcClient.StatementSpec spec=jdbc.sql(sql);if(!user.admin()&&sql.contains(":user"))spec=spec.param("user",user.id());if(!user.admin()&&!access.scopes().isEmpty()&&sql.contains(":scopes"))spec=spec.param("scopes",access.scopes());if(!user.admin()&&!access.datasetIds().isEmpty()&&sql.contains(":sharedDatasetIds"))spec=spec.param("sharedDatasetIds",access.datasetIds());if(!user.admin()&&!user.assignedScopes().isEmpty()&&sql.contains(":assignedScopes"))spec=spec.param("assignedScopes",user.assignedScopes());return spec.query((rs,n)->Map.<String,Object>of("name",rs.getString("name"),"value",rs.getLong("value"))).list();}
    @SuppressWarnings("unchecked") private List<Map<String,Object>> list(Map<String,Object> report,String key){Object value=report.get(key);return value instanceof List<?> rows?(List<Map<String,Object>>)(List<?>)rows:List.of();}
    private String csv(String value){String safe=value==null?"":value;if(!safe.isEmpty()&&"=+-@\t\r\n".indexOf(safe.charAt(0))>=0)safe="'"+safe;return '"'+safe.replace("\"","\"\"")+'"';}

    private long templateCount(UserPrincipal user,String type){
        boolean auditAccess=can(user,"template:audit");
        String visible=user.admin()?"TRUE":user.assignedScopes().isEmpty()
                ?"(creator_id=:user OR (visibility='PUBLIC' AND published=TRUE AND enabled=TRUE))"
                :"(creator_id=:user OR (visibility='PUBLIC' AND published=TRUE AND enabled=TRUE)"
                +(auditAccess?" OR (visibility='PUBLIC' AND data_scope_id IN (:scopes) AND (audit_status=0 OR published=TRUE))":"")+")";
        JdbcClient.StatementSpec spec=jdbc.sql("SELECT count(*) FROM tpl_template WHERE deleted=0 AND type=:type AND "+visible)
                .param("type",type);
        if(!user.admin())spec=spec.param("user",user.id());
        if(!user.admin()&&auditAccess&&!user.assignedScopes().isEmpty())spec=spec.param("scopes",user.assignedScopes());
        return spec.query(Long.class).single();
    }

    private long datasetMetric(UserPrincipal user,String expression,String operation){SetView access=access(user,operation);
        String visible=datasetVisible(user,access);
        JdbcClient.StatementSpec spec=jdbc.sql("SELECT "+expression+" FROM data_dataset d WHERE d.deleted=0 AND "+visible);
        if(!user.admin())spec=spec.param("user",user.id());
        if(!user.admin()&&!access.scopes().isEmpty())spec=spec.param("scopes",access.scopes());if(!user.admin()&&!access.datasetIds().isEmpty())spec=spec.param("sharedDatasetIds",access.datasetIds());
        return spec.query(Long.class).single();
    }

    private long recordMetric(UserPrincipal user,String metric,String operation){SetView access=access(user,operation);
        String datasets=datasetVisible(user,access);
        boolean review=can(user,"dataset:audit")&&!user.assignedScopes().isEmpty();
        String visibility=user.admin()?"TRUE":"(w.owner_id=:user OR w.status='PUBLISHED'"+(review?" OR d.data_scope_id IN (:assignedScopes)":"")+")";
        String status=switch(metric){case "PUBLISHED"->"w.status='PUBLISHED'";case "PUBLISHING"->"w.status IN ('PENDING_REVIEW','APPROVED')";default->"w.status<>'DELETED'";};
        if("PUBLISHING".equals(metric)&&!review)status="("+status+" AND w.owner_id=:user)";
        JdbcClient.StatementSpec spec=jdbc.sql("SELECT count(*) FROM dataset_record_workflow w JOIN data_dataset d ON d.id=w.dataset_id WHERE d.deleted=0 AND "+datasets+" AND "+visibility+" AND "+status);
        if(!user.admin())spec=spec.param("user",user.id());
        if(!user.admin()&&!access.scopes().isEmpty())spec=spec.param("scopes",access.scopes());if(!user.admin()&&!access.datasetIds().isEmpty())spec=spec.param("sharedDatasetIds",access.datasetIds());if(!user.admin()&&review)spec=spec.param("assignedScopes",user.assignedScopes());
        return spec.query(Long.class).single();
    }

    private List<Map<String,Object>> deviceTrend(UserPrincipal user){return deviceTrend(user,access(user,"READ"));}
    private List<Map<String,Object>> deviceTrend(UserPrincipal user,SetView access){
        String scope=user.admin()?"TRUE":access.scopes().isEmpty()?"FALSE":"d.data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec trend=jdbc.sql("SELECT to_char(date_trunc('day',m.measured_time),'MM-DD') AS bucket_day,count(*) AS metric_count FROM device_measurement m JOIN device d ON d.id=m.device_id WHERE m.measured_time>=now()-interval '7 days' AND "+scope+" GROUP BY date_trunc('day',m.measured_time) ORDER BY date_trunc('day',m.measured_time)");
        if(!user.admin()&&!access.scopes().isEmpty())trend=trend.param("scopes",access.scopes());
        return trend.query((rs,n)->Map.<String,Object>of("day",rs.getString("bucket_day"),"value",rs.getLong("metric_count"))).list();
    }

    public Map<String, Object> search(String keyword) {
        UserPrincipal user = CurrentUser.require();
        Map<String,Object> result=new LinkedHashMap<>();
        if (keyword == null || keyword.trim().length() < 2) return result;
        String scope = user.admin() ? "TRUE" : user.dataScopes().isEmpty() ? "FALSE" : "data_scope_id IN (:scopes)";
        if(user.admin()||user.permissions().contains("template:read")) result.put("templates",quickSearch(
                "SELECT id,name,coalesce(description,'') secondary FROM tpl_template WHERE deleted=0 AND enabled=TRUE AND "
                        +(user.admin()?"TRUE":"(creator_id=:userId OR (visibility='PUBLIC' AND published=TRUE))")
                        +" AND (lower(name) LIKE lower(concat('%',:keyword,'%')) OR lower(coalesce(description,'')) LIKE lower(concat('%',:keyword,'%'))) ORDER BY updated_time DESC NULLS LAST,id DESC LIMIT 10",keyword,user,true));
        SetView readAccess=access(user,"READ");if(user.admin()||user.permissions().contains("dataset:read")) result.put("datasets",quickSearch(
                "SELECT id,name,coalesce(description,'') secondary FROM data_dataset WHERE deleted=0 AND "
                        +(user.admin()?"TRUE":"("+(readAccess.scopes().isEmpty()?"FALSE":"data_scope_id IN (:scopes)")+(readAccess.datasetIds().isEmpty()?"":" OR id IN (:sharedDatasetIds)")+")")
                        +" AND (lower(name) LIKE lower(concat('%',:keyword,'%')) OR lower(coalesce(description,'')) LIKE lower(concat('%',:keyword,'%'))) ORDER BY updated_time DESC NULLS LAST,id DESC LIMIT 10",keyword,user,false,readAccess));
        if(user.admin()||user.permissions().contains("trace:read")) result.put("entities",quickSearch(
                "SELECT id,entity_name name,entity_code secondary FROM trace_entity WHERE deleted=0 AND "+scope
                +" AND (lower(entity_name) LIKE lower(concat('%',:keyword,'%')) OR lower(entity_code) LIKE lower(concat('%',:keyword,'%'))) ORDER BY updated_time DESC NULLS LAST,id DESC LIMIT 10",keyword,user,false,readAccess));
        int hits=result.values().stream().mapToInt(value->((List<?>)value).size()).sum();
        audit.record("SEARCH","SEARCH","执行顶部快捷检索",Map.of("keywordLength",keyword.trim().length(),
                "resourceTypes",result.keySet(),"hitCount",hits));
        return result;
    }

    private List<Map<String,Object>> quickSearch(String sql,String keyword,UserPrincipal user,boolean creatorVisible){return quickSearch(sql,keyword,user,creatorVisible,access(user,"READ"));}
    private List<Map<String,Object>> quickSearch(String sql,String keyword,UserPrincipal user,boolean creatorVisible,SetView access) {
        JdbcClient.StatementSpec spec=jdbc.sql(sql).param("keyword",keyword.trim());
        if(!user.admin()&&creatorVisible)spec=spec.param("userId",user.id());
        if (!user.admin() && !access.scopes().isEmpty()&&sql.contains(":scopes")) spec=spec.param("scopes",access.scopes());if(!user.admin()&&!access.datasetIds().isEmpty()&&sql.contains(":sharedDatasetIds"))spec=spec.param("sharedDatasetIds",access.datasetIds());
        return spec.query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"name",rs.getString("name"),
                "secondary",rs.getString("secondary")==null?"":rs.getString("secondary"))).list();
    }

    private long count(String table, String base, UserPrincipal user) { return metric(table, "count(*)", base, user); }
    private long metric(String table, String expression, String base, UserPrincipal user) {
        String scope = user.admin() ? "TRUE" : user.dataScopes().isEmpty() ? "FALSE" : "data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec spec=jdbc.sql("SELECT "+expression+" FROM "+table+" WHERE "+base+" AND "+scope);
        if(!user.admin()&&!user.dataScopes().isEmpty()) spec=spec.param("scopes",user.dataScopes());
        return spec.query(Long.class).single();
    }
    private SetView access(UserPrincipal user,String operation){if(user.admin())return new SetView(user.dataScopes(),java.util.Set.of());java.util.Set<Long> scopes="READ".equals(operation)?new java.util.LinkedHashSet<>(user.dataScopes()):new java.util.LinkedHashSet<>(user.assignedScopes());if(!"READ".equals(operation))scopes.addAll(sharing.sharedResourceIds("DATA_SCOPE",operation));return new SetView(scopes,sharing.sharedResourceIds("DATASET",operation));}
    private String datasetVisible(UserPrincipal user,SetView access){if(user.admin())return "TRUE";return "("+(access.scopes().isEmpty()?"FALSE":"d.data_scope_id IN (:scopes)")+(access.datasetIds().isEmpty()?"":" OR d.id IN (:sharedDatasetIds)")+")";}
    private record SetView(java.util.Set<Long> scopes,java.util.Set<Long> datasetIds){}
}
