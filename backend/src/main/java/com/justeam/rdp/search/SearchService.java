package com.justeam.rdp.search;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.sharing.SharingService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Service
public class SearchService {
    private static final Set<String> RESOURCES = Set.of("DATASET", "RECORD", "TRACE", "DEVICE", "TEMPLATE");
    private static final Set<String> SORTS = Set.of("CREATED_TIME", "UPDATED_TIME", "NAME", "TYPE", "STATUS");
    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final AuditService audit;
    private final StringRedisTemplate redis;
    private final SharingService sharing;

    public SearchService(JdbcClient jdbc, JsonSupport json, AuditService audit, StringRedisTemplate redis,SharingService sharing) {
        this.jdbc = jdbc; this.json = json; this.audit = audit; this.redis=redis;this.sharing=sharing;
    }

    public PageResponse<Map<String, Object>> search(List<String> requestedResources,
                                                     List<SearchController.Condition> rawConditions,
                                                     Instant from, Instant to, int pageNum, int pageSize,
                                                     String rawSortBy, String rawSortOrder) {
        long started = System.nanoTime();
        if (from != null && to != null && !from.isBefore(to)) throw BusinessException.badRequest("开始时间必须早于结束时间");
        int page = Math.max(1, pageNum), size = Math.max(10, Math.min(pageSize, 50));
        String sortBy = normalize(rawSortBy, SORTS, "CREATED_TIME", "排序字段不正确");
        String sortOrder = normalize(rawSortOrder, Set.of("ASC", "DESC"), "DESC", "排序方向不正确");
        List<SearchController.Condition> conditions = normalizeConditions(rawConditions);
        UserPrincipal user = CurrentUser.require();
        Set<String> resources = allowedResources(requestedResources, user);
        int required = (int) Math.min(Integer.MAX_VALUE, (long) page * size);
        List<Map<String, Object>> candidates = new ArrayList<>();
        long total = 0;
        for (String resource : resources) {
            SearchChunk chunk = "RECORD".equals(resource)
                    ? searchRecords(user, conditions, from, to, required, sortBy, sortOrder)
                    : searchPostgres(resource, user, conditions, from, to, required, sortBy, sortOrder);
            total += chunk.total(); candidates.addAll(chunk.rows());
        }
        candidates.sort(comparator(sortBy, sortOrder));
        long offsetValue = (long) (page - 1) * size;
        int offset=(int)offsetValue;
        List<Map<String, Object>> rows = offset >= candidates.size() ? List.of()
                : new ArrayList<>(candidates.subList(offset, Math.min(candidates.size(), offset + size)));
        rows.forEach(value->value.remove("_sortId"));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        Map<String,Object> auditDetails=new LinkedHashMap<>();auditDetails.put("resourceTypes",resources);
        auditDetails.put("conditions",auditedConditions(conditions));auditDetails.put("from",from==null?null:from.toString());
        auditDetails.put("to",to==null?null:to.toString());auditDetails.put("pageNum",page);auditDetails.put("pageSize",size);
        auditDetails.put("sortBy",sortBy);auditDetails.put("sortOrder",sortOrder);auditDetails.put("hitCount",total);
        auditDetails.put("returnedCount",rows.size());auditDetails.put("elapsedMs",elapsedMs);
        audit.record("SEARCH", "SEARCH", "执行高级组合检索", auditDetails);
        if(page==1){saveHistory(user,resources,conditions,from,to,size,sortBy,sortOrder);if(total>0)recordPopularTerms(user,conditions);}
        return PageResponse.of(total, page, size, rows);
    }

    public List<Map<String,Object>> history(){UserPrincipal user=CurrentUser.require();try{
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> values=redis.opsForZSet().reverseRangeWithScores(historyKey(user.id()),0,19);
        if(values==null)return List.of();List<Map<String,Object>> result=new ArrayList<>();for(var tuple:values){Map<String,Object> entry=new LinkedHashMap<>(json.map(tuple.getValue()));entry.put("searchedAt",tuple.getScore()==null?null:Instant.ofEpochMilli(tuple.getScore().longValue()).toString());result.add(entry);}return result;
    }catch(Exception ignored){return List.of();}}

    public void clearHistory(){redis.delete(historyKey(CurrentUser.require().id()));}

    public List<Map<String,Object>> suggestions(String rawPrefix,int limit){
        UserPrincipal user=CurrentUser.require();String prefix=rawPrefix==null?"":rawPrefix.trim().toLowerCase(Locale.ROOT);
        Map<String,Suggestion> merged=new HashMap<>();List<String> keys=List.of(personalSuggestionKey(user.id()));
        try{
            for(String key:keys){Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> values=redis.opsForZSet().reverseRangeWithScores(key,0,99);if(values==null)continue;
                for(var tuple:values){String term=tuple.getValue();if(term==null||!term.toLowerCase(Locale.ROOT).startsWith(prefix))continue;double score=tuple.getScore()==null?0:tuple.getScore();String normalized=term.toLowerCase(Locale.ROOT);Suggestion previous=merged.get(normalized);merged.put(normalized,new Suggestion(term,(previous==null?0:previous.score())+score));}}
        }catch(Exception ignored){return List.of();}
        return merged.values().stream().sorted(Comparator.comparingDouble(Suggestion::score).reversed().thenComparing(Suggestion::value))
                .limit(Math.max(1,Math.min(20,limit))).map(value->Map.<String,Object>of("value",value.value(),"score",value.score())).toList();
    }

    private void saveHistory(UserPrincipal user,Set<String> resources,List<SearchController.Condition> conditions,Instant from,Instant to,int pageSize,String sortBy,String sortOrder){try{
        Map<String,Object> value=new LinkedHashMap<>();value.put("resourceTypes",resources);value.put("conditions",conditions.stream().map(c->Map.of("field",c.field(),"value",c.value(),"matchMode",c.matchMode())).toList());value.put("from",from==null?null:from.toString());value.put("to",to==null?null:to.toString());value.put("pageSize",pageSize);value.put("sortBy",sortBy);value.put("sortOrder",sortOrder);
        String key=historyKey(user.id()),member=json.write(value);redis.opsForZSet().add(key,member,System.currentTimeMillis());Long size=redis.opsForZSet().size(key);if(size!=null&&size>20)redis.opsForZSet().removeRange(key,0,size-21);redis.expire(key,Duration.ofDays(30));
    }catch(Exception ignored){}}
    private String historyKey(long userId){return "search:history:"+userId;}

    private void recordPopularTerms(UserPrincipal user,List<SearchController.Condition> conditions){try{
        for(SearchController.Condition condition:conditions){if(Set.of("CATEGORY","TYPE","STATUS").contains(condition.field()))continue;String term=condition.value().trim();if(term.length()<2||term.length()>50)continue;
            incrementSuggestion(personalSuggestionKey(user.id()),term);}
    }catch(Exception ignored){}}
    private void incrementSuggestion(String key,String term){redis.opsForZSet().incrementScore(key,term,1);Long count=redis.opsForZSet().size(key);if(count!=null&&count>500)redis.opsForZSet().removeRange(key,0,count-501);redis.expire(key,Duration.ofDays(30));}
    private String personalSuggestionKey(long userId){return "search:suggest:user:"+userId;}

    private SearchChunk searchPostgres(String resource, UserPrincipal user,
                                       List<SearchController.Condition> conditions, Instant from, Instant to,
                                       int limit, String sortBy, String sortOrder) {
        SqlResource sql = sqlResource(resource, user);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(sql.baseWhere());
        int index = 0;
        for (SearchController.Condition condition : conditions) {
            String name = "condition" + index++;
            where.append(" AND ").append(conditionClause(resource, sql, condition, name));
            params.put(name, condition.value());
        }
        if (from != null) { where.append(" AND ").append(sql.timeExpression()).append(">=:from"); params.put("from", java.sql.Timestamp.from(from)); }
        if (to != null) { where.append(" AND ").append(sql.timeExpression()).append("<:to"); params.put("to", java.sql.Timestamp.from(to)); }
        bindScope(params, user, sql.creatorVisible());
        if(sql.baseWhere().contains(":sharedDatasetIds"))params.put("sharedDatasetIds",sharing.sharedResourceIds("DATASET","READ"));
        if("TEMPLATE".equals(resource)&&!sql.baseWhere().contains(":scopes"))params.remove("scopes");
        if(!sql.baseWhere().contains(":assignedScopes"))params.remove("assignedScopes");
        String headline=headlineTerms(conditions);if(headline!=null)params.put("headline",headline);
        String fromSql = " FROM " + sql.tableAndAlias() + " WHERE " + where;
        JdbcClient.StatementSpec count = bind(jdbc.sql("SELECT count(*)" + fromSql), params);
        long total = count.query(Long.class).single();
        String order = orderExpression(sql, sortBy) + " " + sortOrder + " NULLS LAST," + sql.idExpression() + " " + sortOrder;
        String highlight=headline==null?",NULL::text AS highlight_html ":",ts_headline('simple',rdp_headline_text("+escapedDocument(resource)+"),plainto_tsquery('simple',rdp_headline_text(:headline)),'StartSel=<mark>,StopSel=</mark>,MaxWords=35,MinWords=10,ShortWord=1,MaxFragments=2,FragmentDelimiter= … ') AS highlight_html ";
        JdbcClient.StatementSpec query = bind(jdbc.sql(sql.select() + highlight + fromSql + " ORDER BY " + order + " LIMIT :limit"), params)
                .param("limit", limit);
        return new SearchChunk(total, query.query((rs, n) -> resultRow(rs)).list());
    }

    private SearchChunk searchRecords(UserPrincipal user, List<SearchController.Condition> conditions,
                                      Instant from, Instant to, int limit, String sortBy, String sortOrder) {
        List<DatasetIndex> datasets = accessibleDatasets(user);
        List<Map<String, Object>> rows = new ArrayList<>();
        long total = 0;
        for (DatasetIndex dataset : datasets) {
            if (!datasetMetadataMatches(dataset, conditions)) continue;
            Map<String,Object> params=new LinkedHashMap<>();params.put("dataset",dataset.id());
            StringBuilder where=new StringBuilder("w.dataset_id=:dataset AND w.status NOT IN ('DELETED','CORRECTING','DELETING','ARCHIVING','ARCHIVED','RESTORING')");
            if(!canReviewDataset(user,dataset.scopeId())){where.append(" AND (w.owner_id=:actor OR w.status='PUBLISHED')");params.put("actor",user.id());}
            String visibleWhere=where.toString();
            long pending=bind(jdbc.sql("SELECT count(*) FROM dataset_record_workflow w WHERE "+visibleWhere+" AND w.search_projection_ready=FALSE"),params).query(Long.class).single();
            if(pending>0)throw new BusinessException(503,"历史记录检索索引正在构建，请稍后重试（剩余 "+pending+" 条）");
            if(from!=null){where.append(" AND w.record_created_time>=:from");params.put("from",java.sql.Timestamp.from(from));}
            if(to!=null){where.append(" AND w.record_created_time<:to");params.put("to",java.sql.Timestamp.from(to));}
            int conditionIndex=0;
            for(SearchController.Condition condition:conditions){
                if("CATEGORY".equals(condition.field())||"TYPE".equals(condition.field()))continue;
                String valueName="recordValue"+conditionIndex;params.put(valueName,condition.value());
                if("STATUS".equals(condition.field())){
                    where.append(" AND ").append("EXACT".equals(condition.matchMode())
                            ?"lower(trim(w.status))=lower(trim(:"+valueName+"))"
                            :"lower(w.status) LIKE lower(concat('%',:"+valueName+",'%'))");
                }else if("KEYWORD".equals(condition.field())&&"FUZZY".equals(condition.matchMode())){
                    where.append(" AND w.search_vector @@ plainto_tsquery('simple',rdp_search_tokens(:").append(valueName).append("))");
                }else{
                    List<String> paths=recordPaths(dataset.fields(),condition.field());
                    if(paths.isEmpty()){where.append(" AND FALSE");conditionIndex++;continue;}
                    List<String> alternatives=new ArrayList<>();int pathIndex=0;
                    for(String path:paths){String pathName="recordPath"+conditionIndex+"_"+pathIndex++;params.put(pathName,path.substring("data.".length()));
                        String expression="coalesce(jsonb_extract_path_text(w.search_data,:"+pathName+"),'')";
                        alternatives.add("EXACT".equals(condition.matchMode())
                                ?"lower(trim("+expression+"))=lower(trim(:"+valueName+"))"
                                :"lower("+expression+") LIKE lower(concat('%',:"+valueName+",'%'))");}
                    where.append(" AND (").append(String.join(" OR ",alternatives)).append(')');
                }
                conditionIndex++;
            }
            String fromSql=" FROM dataset_record_workflow w WHERE "+where;
            total+=bind(jdbc.sql("SELECT count(*)"+fromSql),params).query(Long.class).single();
            String order=recordOrder(sortBy)+" "+sortOrder+" NULLS LAST,w.record_id "+sortOrder;
            String headline=headlineTerms(conditions);if(headline!=null)params.put("headline",headline);
            String highlight=headline==null?"NULL::text":"ts_headline('simple',rdp_headline_text("+escaped("w.search_data::text")+"),plainto_tsquery('simple',rdp_headline_text(:headline)),'StartSel=<mark>,StopSel=</mark>,MaxWords=35,MinWords=10,ShortWord=1,MaxFragments=2,FragmentDelimiter= … ')";
            JdbcClient.StatementSpec query=bind(jdbc.sql("""
                    SELECT w.record_id,w.status,w.search_data::text AS search_data,
                           w.record_created_time,w.record_updated_time,
                    """+highlight+" AS highlight_html "+fromSql+" ORDER BY "+order+" LIMIT :limit"),params).param("limit",limit);
            rows.addAll(query.query((rs,n)->recordProjectionResult(dataset,rs)).list());
        }
        return new SearchChunk(total, rows);
    }

    private String recordOrder(String sortBy){return switch(sortBy){case "UPDATED_TIME"->"w.record_updated_time";case "STATUS"->"lower(w.status)";case "NAME","TYPE"->"w.record_id";default->"w.record_created_time";};}

    private SqlResource sqlResource(String resource, UserPrincipal user) {
        String scope = scopeClause(user, switch (resource) { case "DATASET", "TEMPLATE" -> true; default -> false; });
        boolean templateAuditor=user.admin()||user.permissions().contains("template:audit");
        String templateVisibility=user.admin()?"TRUE":"(t.creator_id=:userId OR (t.visibility='PUBLIC' AND t.published=TRUE AND t.enabled=TRUE)"
                +(templateAuditor&&!user.assignedScopes().isEmpty()?" OR (t.visibility='PUBLIC' AND t.data_scope_id IN (:assignedScopes) AND (t.audit_status=0 OR t.published=TRUE))":"")+")";
        String templateStatus="CASE WHEN t.published THEN 'PUBLISHED' WHEN t.audit_status=0 THEN 'PENDING_REVIEW' WHEN t.audit_status=2 THEN 'REJECTED' ELSE 'DRAFT' END";
        return switch (resource) {
            case "DATASET" -> new SqlResource("data_dataset d", "d.id", "d.created_time", "d.updated_time", "d.name", "d.category", "d.status::text", true,
                    "d.deleted=0 AND (" + scope + (sharing.sharedResourceIds("DATASET","READ").isEmpty()?"":" OR d.id IN (:sharedDatasetIds)")+")",
                    "SELECT d.id::text AS id,lpad(d.id::text,20,'0') AS sort_id,'DATASET' AS resource_type,d.name AS title,coalesce(d.tags,'') AS code,coalesce(d.description,'') AS summary,coalesce(d.category,'') AS category,d.status::text AS status,d.data_scope_id,d.created_time,d.updated_time,'/datasets/'||d.id AS route,jsonb_build_object('recordCount',d.data_count,'tags',d.tags) AS attributes ");
            case "TRACE" -> new SqlResource("trace_entity e", "e.id", "e.created_time", "e.updated_time", "e.entity_name", "e.entity_type", "''", false,
                    "e.deleted=0 AND " + scope,
                    "SELECT e.id::text AS id,lpad(e.id::text,20,'0') AS sort_id,'TRACE' AS resource_type,e.entity_name AS title,e.entity_code AS code,e.properties::text AS summary,e.entity_type AS category,'' AS status,e.data_scope_id,e.created_time,e.updated_time,'/trace?root='||e.id AS route,e.properties AS attributes ");
            case "DEVICE" -> new SqlResource("device d", "d.id", "d.created_time", "d.updated_time", "d.device_name", "d.device_type", "d.status", false,
                    scope,
                    "SELECT d.id::text AS id,lpad(d.id::text,20,'0') AS sort_id,'DEVICE' AS resource_type,d.device_name AS title,d.device_code AS code,coalesce(d.model,'')||' · '||coalesce(d.protocol,'') AS summary,d.device_type AS category,d.status,d.data_scope_id,d.created_time,d.updated_time,'/devices?focus='||d.id AS route,jsonb_build_object('model',d.model,'protocol',d.protocol,'lastSeenTime',d.last_seen_time) AS attributes ");
            case "TEMPLATE" -> new SqlResource("tpl_template t", "t.id", "t.created_time", "t.updated_time", "t.name", "t.type", templateStatus, true,
                    "t.deleted=0 AND "+templateVisibility,
                    "SELECT t.id::text AS id,lpad(t.id::text,20,'0') AS sort_id,'TEMPLATE' AS resource_type,t.name AS title,coalesce(t.tag,'') AS code,coalesce(t.description,'') AS summary,t.type AS category,"+templateStatus+" AS status,t.data_scope_id,t.created_time,t.updated_time,'/templates?focus='||t.id AS route,jsonb_build_object('visibility',t.visibility,'version',t.version,'published',t.published) AS attributes ");
            default -> throw BusinessException.badRequest("不支持的资源类型");
        };
    }

    private String conditionClause(String resource, SqlResource sql, SearchController.Condition condition, String parameter) {
        if ("EXACT".equals(condition.matchMode())) {
            return exactClause(resource, sql, condition.field(), ":" + parameter);
        }
        if("KEYWORD".equals(condition.field()))
            return searchVector(resource)+" @@ plainto_tsquery('simple',rdp_search_tokens(:"+parameter+"))";
        String value = switch (resource) {
            case "DATASET" -> "concat_ws(' ',d.name,d.description,d.tags,d.category,d.field_definition::text)";
            case "TRACE" -> "concat_ws(' ',e.entity_code,e.entity_name,e.entity_type,e.properties::text,e.source_system,e.source_record_id)";
            case "DEVICE" -> "concat_ws(' ',d.device_code,d.device_name,d.device_type,d.model,d.protocol,d.status)";
            case "TEMPLATE" -> "concat_ws(' ',t.name,t.tag,t.description,t.source,t.type,t.content::text,t.schema_definition::text)";
            default -> "''";
        };
        String fieldExpression = switch (condition.field()) {
            case "KEYWORD" -> value;
            case "CATEGORY" -> sql.categoryExpression();
            case "TYPE" -> sql.typeExpression();
            case "STATUS" -> sql.statusExpression();
            case "DEVICE" -> "DEVICE".equals(resource) ? value : "TRACE".equals(resource)
                    ? "CASE WHEN upper(e.entity_type) LIKE '%DEVICE%' OR upper(e.entity_type) LIKE '%EQUIPMENT%' OR e.entity_type='设备' THEN " + value + " ELSE '' END" : "''";
            case "MATERIAL" -> semanticExpression(resource, value, "MATERIAL", "材料");
            case "PROCESS" -> semanticExpression(resource, value, "PROCESS", "工艺");
            case "BATCH" -> semanticExpression(resource, value, "BATCH", "批次");
            case "PRODUCT_MODEL" -> semanticExpression(resource, value, "PRODUCT_MODEL", "产品型号");
            default -> "''";
        };
        return match(fieldExpression, ":" + parameter, condition.matchMode());
    }

    private String exactClause(String resource, SqlResource sql, String field, String parameter) {
        if ("CATEGORY".equals(field) || "TYPE".equals(field)) return equal(sql.typeExpression(), parameter);
        if ("STATUS".equals(field)) return equal(sql.statusExpression(), parameter);
        if ("TRACE".equals(resource) && Set.of("MATERIAL","PROCESS","BATCH","DEVICE","PRODUCT_MODEL").contains(field)) {
            String expected = switch (field) { case "MATERIAL" -> "MATERIAL"; case "PROCESS" -> "PROCESS"; case "BATCH" -> "BATCH"; case "DEVICE" -> "DEVICE"; default -> "PRODUCT"; };
            String chinese=switch(field){case "MATERIAL"->"材料";case "PROCESS"->"工艺";case "BATCH"->"批次";case "DEVICE"->"设备";default->"产品型号";};
            return "((upper(e.entity_type) LIKE '%" + expected + "%' OR e.entity_type='"+chinese+"') AND ("
                    + equal("e.entity_code",parameter)+" OR "+equal("e.entity_name",parameter)+"))";
        }
        if ("DEVICE".equals(resource) && ("DEVICE".equals(field) || "KEYWORD".equals(field))) {
            return exactAny(parameter,"d.device_code","d.device_name","d.model","d.device_type","d.protocol","d.status");
        }
        if(Set.of("MATERIAL","PROCESS","BATCH","DEVICE","PRODUCT_MODEL").contains(field))return "FALSE";
        if ("DATASET".equals(resource)) return exactAny(parameter,"d.name","d.description","d.tags","d.category");
        if ("TRACE".equals(resource)) return exactAny(parameter,"e.entity_code","e.entity_name","e.entity_type","e.source_record_id","e.source_system");
        if ("TEMPLATE".equals(resource)) return exactAny(parameter,"t.name","t.tag","t.description","t.source","t.type");
        return "FALSE";
    }

    private String exactAny(String parameter,String... expressions){return "("+java.util.Arrays.stream(expressions).map(value->equal(value,parameter)).collect(java.util.stream.Collectors.joining(" OR "))+")";}
    private String equal(String expression,String parameter){return "lower(trim(coalesce("+expression+",'')))=lower(trim("+parameter+"))";}

    private String semanticExpression(String resource, String value, String entityType, String chineseName) {
        if ("TRACE".equals(resource)) {
            String marker = "PRODUCT_MODEL".equals(entityType) ? "PRODUCT" : entityType;
            return "CASE WHEN upper(e.entity_type) LIKE '%" + marker + "%' OR e.entity_type='" + chineseName + "' THEN " + value + " ELSE '' END";
        }
        return "''";
    }

    private String match(String expression, String parameter, String mode) {
        return "EXACT".equals(mode) ? "lower(trim(" + expression + "))=lower(trim(" + parameter + "))"
                : "lower(" + expression + ") LIKE lower(concat('%'," + parameter + ",'%'))";
    }

    private String searchVector(String resource){return switch(resource){case "DATASET","DEVICE"->"d.search_vector";case "TRACE"->"e.search_vector";case "TEMPLATE"->"t.search_vector";default->throw BusinessException.badRequest("资源类型不支持全文检索");};}
    private String document(String resource){return switch(resource){case "DATASET"->"concat_ws(' ',d.name,d.description,d.tags,d.category,d.field_definition::text)";case "TRACE"->"concat_ws(' ',e.entity_code,e.entity_name,e.entity_type,e.properties::text,e.source_system,e.source_record_id)";case "DEVICE"->"concat_ws(' ',d.device_code,d.device_name,d.device_type,d.model,d.protocol,d.status)";case "TEMPLATE"->"concat_ws(' ',t.name,t.tag,t.description,t.source,t.type,t.content::text,t.schema_definition::text)";default->"''";};}
    private String escapedDocument(String resource){return escaped(document(resource));}
    private String escaped(String expression){return "replace(replace(replace("+expression+",'&','&amp;'),'<','&lt;'),'>','&gt;')";}
    private String headlineTerms(List<SearchController.Condition> conditions){String value=conditions.stream().filter(c->!Set.of("CATEGORY","TYPE","STATUS").contains(c.field())).map(SearchController.Condition::value).reduce((a,b)->a+" "+b).orElse(null);return value==null||value.isBlank()?null:value;}

    private String orderExpression(SqlResource sql, String sortBy) {
        return switch (sortBy) {
            case "UPDATED_TIME" -> "coalesce(" + sql.updatedExpression() + "," + sql.timeExpression() + ")";
            case "NAME" -> "lower(" + sql.nameExpression() + ")";
            case "TYPE" -> "lower(" + sql.typeExpression() + ")";
            case "STATUS" -> "lower(" + sql.statusExpression() + ")";
            default -> sql.timeExpression();
        };
    }

    private Comparator<Map<String, Object>> comparator(String sortBy, String sortOrder) {
        String key = switch (sortBy) { case "UPDATED_TIME" -> "updatedTime"; case "NAME" -> "title"; case "TYPE" -> "category"; case "STATUS" -> "status"; default -> "createdTime"; };
        Comparator<String> scalar = "DESC".equals(sortOrder) ? Comparator.reverseOrder() : Comparator.naturalOrder();
        Comparator<Map<String,Object>> comparator = Comparator.comparing(v -> comparable(v.get(key)), Comparator.nullsLast(scalar));
        Comparator<Map<String,Object>> stable = Comparator.comparing((Map<String,Object> v) -> String.valueOf(v.get("resourceType")))
                .thenComparing((Map<String,Object> v) -> String.valueOf(v.get("_sortId")));
        return comparator.thenComparing("DESC".equals(sortOrder) ? stable.reversed() : stable);
    }

    private String comparable(Object value) {
        if(value==null)return null;
        if(value instanceof Instant instant)return String.format("%020d",instant.toEpochMilli());
        if(value instanceof java.util.Date date)return String.format("%020d",date.getTime());
        if(value instanceof java.time.OffsetDateTime time)return String.format("%020d",time.toInstant().toEpochMilli());
        return String.valueOf(value).toLowerCase(Locale.ROOT);
    }

    private Set<String> allowedResources(List<String> requested, UserPrincipal user) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : requested) {
            String resource = normalize(raw, RESOURCES, null, "资源类型不正确");
            String permission = switch (resource) { case "DATASET", "RECORD" -> "dataset:read"; case "TRACE" -> "trace:read"; case "DEVICE" -> "device:read"; case "TEMPLATE" -> "template:read"; default -> ""; };
            if (user.admin() || user.permissions().contains(permission)) result.add(resource);
        }
        return result;
    }

    private List<SearchController.Condition> normalizeConditions(List<SearchController.Condition> raw) {
        if (raw == null) return List.of();
        List<SearchController.Condition> result = new ArrayList<>();
        for (SearchController.Condition value : raw) {
            String text = value.value() == null ? "" : value.value().trim();
            if (text.isEmpty()) continue;
            result.add(new SearchController.Condition(value.field().toUpperCase(Locale.ROOT), text,
                    value.matchMode().toUpperCase(Locale.ROOT)));
        }
        return result;
    }

    private List<DatasetIndex> accessibleDatasets(UserPrincipal user) {
        Set<Long> shared=sharing.sharedResourceIds("DATASET","READ");String scope = "("+scopeClause(user, true)+(shared.isEmpty()?"":" OR d.id IN (:sharedDatasetIds)")+")";
        JdbcClient.StatementSpec query = jdbc.sql("SELECT id,name,category,status,data_scope_id,field_definition::text AS fields FROM data_dataset d WHERE deleted=0 AND " + scope)
                .param("userId", user.id());
        if (!user.admin() && !user.dataScopes().isEmpty()) query = query.param("scopes", user.dataScopes());
        if(!user.admin()&&!shared.isEmpty())query=query.param("sharedDatasetIds",shared);
        return query.query((rs,n)->new DatasetIndex(rs.getLong("id"),rs.getString("name"),rs.getString("category"),
                rs.getInt("status"),rs.getLong("data_scope_id"),readFields(rs.getString("fields")))).list();
    }

    private boolean datasetMetadataMatches(DatasetIndex dataset, List<SearchController.Condition> conditions) {
        for (SearchController.Condition condition : conditions) {
            String actual = switch (condition.field()) { case "CATEGORY" -> dataset.category(); case "TYPE" -> dataset.category(); default -> null; };
            if (actual != null && !matches(actual, condition.value(), condition.matchMode())) return false;
        }
        return true;
    }

    private List<String> recordPaths(List<Map<String,Object>> fields, String semantic) {
        List<String> paths = new ArrayList<>();
        for (Map<String,Object> field : fields) {
            String key = String.valueOf(field.get("key")), label = String.valueOf(field.get("label"));
            if ("KEYWORD".equals(semantic) || aliases(semantic).stream().anyMatch(alias -> normalized(key + label).contains(alias))) paths.add("data." + key);
        }
        return paths;
    }

    private Set<String> aliases(String semantic) {
        return switch (semantic) {
            case "MATERIAL" -> Set.of("material","材料","物料"); case "PROCESS" -> Set.of("process","工艺","工序");
            case "BATCH" -> Set.of("batch","批次","批号"); case "DEVICE" -> Set.of("device","equipment","设备");
            case "PRODUCT_MODEL" -> Set.of("productmodel","model","产品型号","规格型号");
            case "STATUS" -> Set.of("status","状态"); default -> Set.of();
        };
    }

    private String normalized(String value) { return value == null ? "" : value.replaceAll("[\\s_\\-]", "").toLowerCase(Locale.ROOT); }
    private List<Map<String,Object>> readFields(String value){try{return json.mapper().readValue(value,new com.fasterxml.jackson.core.type.TypeReference<>(){});}catch(Exception ex){throw new IllegalStateException("数据集字段定义损坏",ex);}}
    private boolean matches(String actual,String expected,String mode){if(actual==null)return false;return "EXACT".equals(mode)?actual.trim().equalsIgnoreCase(expected.trim()):actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));}

    private boolean canReviewDataset(UserPrincipal user,long scopeId) {
        return user.admin() || user.permissions().contains("dataset:audit") && user.assignedScopes().contains(scopeId);
    }

    private Map<String,Object> recordProjectionResult(DatasetIndex dataset,ResultSet rs)throws SQLException{
        String id=rs.getString("record_id");Map<String,Object> data=json.map(rs.getString("search_data"));
        Map<String,Object> value=new LinkedHashMap<>();value.put("id",id);value.put("_sortId",id);
        value.put("resourceType","RECORD");value.put("title",dataset.name()+" · 记录 "+id.substring(0,Math.min(8,id.length())));
        value.put("code",id);value.put("summary",json.write(data));value.put("category",dataset.category());
        value.put("status",rs.getString("status"));value.put("dataScopeId",dataset.scopeId());
        value.put("highlightHtml",compactHeadline(rs.getString("highlight_html")));
        value.put("createdTime",rs.getObject("record_created_time"));value.put("updatedTime",rs.getObject("record_updated_time"));
        value.put("route","/datasets/"+dataset.id()+"?record="+id);value.put("attributes",Map.of("datasetId",dataset.id(),"data",data));
        return value;
    }

    private Map<String,Object> resultRow(ResultSet rs) throws SQLException {
        Map<String,Object> value=new LinkedHashMap<>();value.put("id",rs.getString("id"));value.put("_sortId",rs.getString("sort_id"));value.put("resourceType",rs.getString("resource_type"));
        value.put("title",rs.getString("title"));value.put("code",rs.getString("code"));value.put("summary",rs.getString("summary"));
        value.put("category",rs.getString("category"));value.put("status",rs.getString("status"));value.put("dataScopeId",rs.getLong("data_scope_id"));
        value.put("highlightHtml",compactHeadline(rs.getString("highlight_html")));
        Object created=rs.getObject("created_time"),updated=rs.getObject("updated_time");value.put("createdTime",created);value.put("updatedTime",updated==null?created:updated);value.put("route",rs.getString("route"));
        value.put("attributes",json.map(rs.getString("attributes")));return value;
    }

    private String scopeClause(UserPrincipal user, boolean creatorVisible) {
        if (user.admin()) return "TRUE";
        if (user.dataScopes().isEmpty()) return "FALSE";
        return "data_scope_id IN (:scopes)";
    }

    private void bindScope(Map<String,Object> params, UserPrincipal user, boolean creatorVisible) {
        if (creatorVisible) params.put("userId", user.id());
        if (!user.admin() && !user.dataScopes().isEmpty()) params.put("scopes", user.dataScopes());
        if(!user.admin()&&!user.assignedScopes().isEmpty())params.put("assignedScopes",user.assignedScopes());
    }

    private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, Map<String,Object> params) { for (var entry:params.entrySet()) spec=spec.param(entry.getKey(),entry.getValue()); return spec; }
    private String normalize(String raw, Set<String> allowed, String fallback, String message) { String value=raw==null||raw.isBlank()?fallback:raw.trim().toUpperCase(Locale.ROOT);if(value==null||!allowed.contains(value))throw BusinessException.badRequest(message);return value; }

    private List<Map<String,Object>> auditedConditions(List<SearchController.Condition> conditions) {
        return conditions.stream().map(condition -> Map.<String,Object>of("field",condition.field(),"matchMode",condition.matchMode(),
                "valueLength",condition.value().length(),"valueDigest",digest(condition.value()))).toList();
    }
    private String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private String compactHeadline(String value){if(value==null)return null;return value.replaceAll("</mark>\\s+<mark>","</mark><mark>").replaceAll("(?<=[一-龥])\\s+(?=[一-龥])","").replaceAll("(?<=[一-龥])\\s+(?=<mark>)","").replaceAll("</mark>\\s+(?=[一-龥])","</mark>");}

    private record SearchChunk(long total,List<Map<String,Object>> rows) {}
    private record Suggestion(String value,double score){}
    private record DatasetIndex(long id,String name,String category,int status,long scopeId,List<Map<String,Object>> fields) {}
    private record SqlResource(String tableAndAlias,String idExpression,String timeExpression,String updatedExpression,
                               String nameExpression,String typeExpression,String statusExpression,boolean creatorVisible,
                               String baseWhere,String select) {
        String categoryExpression(){return typeExpression;}
    }
}
