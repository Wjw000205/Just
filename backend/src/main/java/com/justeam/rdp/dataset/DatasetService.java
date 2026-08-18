package com.justeam.rdp.dataset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.DataScopeService;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.sharing.SharingService;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.FilterOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;

@Service
public class DatasetService {
    private final JdbcClient jdbc;
    private final MongoTemplate mongo;
    private final JsonSupport json;
    private final DataScopeService scopes;
    private final AuditService audit;
    private final CrossStoreOutboxService outbox;
    private final DatasetMutationLock mutationLock;
    private final SharingService sharing;

    public DatasetService(JdbcClient jdbc, MongoTemplate mongo, JsonSupport json, DataScopeService scopes,
                          AuditService audit, CrossStoreOutboxService outbox,DatasetMutationLock mutationLock,SharingService sharing) {
        this.jdbc = jdbc;
        this.mongo = mongo;
        this.json = json;
        this.scopes = scopes;
        this.audit = audit;
        this.outbox = outbox;
        this.mutationLock=mutationLock;
        this.sharing=sharing;
    }

    public PageResponse<Map<String, Object>> list(String keyword, String category, Integer status, int pageNum, int pageSize) {
        UserPrincipal user = CurrentUser.require();
        java.util.Set<Long> sharedIds=sharing.sharedResourceIds("DATASET","READ");
        int page = Math.max(1, pageNum), size = Math.max(1, Math.min(pageSize, 100));
        String scopeClause = user.admin() ? "TRUE" : "("+(user.dataScopes().isEmpty()?"FALSE":"d.data_scope_id IN (:scopes)")
                +(sharedIds.isEmpty()?"":" OR d.id IN (:sharedIds)")+")";
        String where = """
                FROM data_dataset d WHERE d.deleted=0
                AND (CAST(:keyword AS text) IS NULL OR lower(d.name) LIKE lower(concat('%',CAST(:keyword AS text),'%')) OR lower(coalesce(d.tags,'')) LIKE lower(concat('%',CAST(:keyword AS text),'%')))
                AND (CAST(:category AS text) IS NULL OR d.category=CAST(:category AS text)) AND (CAST(:status AS integer) IS NULL OR d.status=CAST(:status AS integer))
                """ + " AND " + scopeClause;
        JdbcClient.StatementSpec count = params(jdbc.sql("SELECT count(*) " + where), keyword, category, status, user,sharedIds);
        long total = count.query(Long.class).single();
        JdbcClient.StatementSpec query = params(jdbc.sql("""
                SELECT d.*,
                EXISTS(SELECT 1 FROM user_favorite f WHERE f.user_id=:userId AND f.target_type='dataset' AND f.target_id=d.id) AS favorited,
                EXISTS(SELECT 1 FROM file_asset fa WHERE fa.business_type='DATASET' AND fa.business_ref=CAST(d.id AS text) AND fa.status='AVAILABLE') AS has_attachments,
                EXISTS(SELECT 1 FROM lifecycle_archive_item ai WHERE ai.dataset_id=d.id AND (ai.state IN ('ARCHIVING','ARCHIVED','RESTORING') OR (ai.state='FAILED' AND ai.archive_path IS NOT NULL))) AS has_recoverable_archive
                """ + where + " ORDER BY d.updated_time DESC NULLS LAST,d.created_time DESC LIMIT :limit OFFSET :offset"),
                keyword, category, status, user,sharedIds).param("limit", size).param("offset", (page - 1) * size);
        return PageResponse.of(total, page, size, query.query(this::row).list());
    }

    public Map<String, Object> get(long id) {
        long userId=CurrentUser.require().id();
        Map<String, Object> value = jdbc.sql("""
                SELECT d.*,
                EXISTS(SELECT 1 FROM user_favorite f WHERE f.user_id=:userId AND f.target_type='dataset' AND f.target_id=d.id) AS favorited,
                EXISTS(SELECT 1 FROM file_asset fa WHERE fa.business_type='DATASET' AND fa.business_ref=CAST(d.id AS text) AND fa.status='AVAILABLE') AS has_attachments,
                EXISTS(SELECT 1 FROM lifecycle_archive_item ai WHERE ai.dataset_id=d.id AND (ai.state IN ('ARCHIVING','ARCHIVED','RESTORING') OR (ai.state='FAILED' AND ai.archive_path IS NOT NULL))) AS has_recoverable_archive
                FROM data_dataset d WHERE d.id=:id AND d.deleted=0
                """)
                .param("userId",userId).param("id", id).query(this::row).optional().orElseThrow(() -> BusinessException.notFound("数据集不存在"));
        long scopeId = ((Number) value.get("dataScopeId")).longValue();
        UserPrincipal user = CurrentUser.require();
        if (!user.admin() && !scopes.canAccess(scopeId)&&!sharing.hasResourceAccess("DATASET",id,"READ")) throw BusinessException.forbidden("无权访问该数据集");
        return value;
    }

    public void requireOperation(Map<String,Object> dataset,String operation){UserPrincipal user=CurrentUser.require();long id=((Number)dataset.get("id")).longValue(),scope=((Number)dataset.get("dataScopeId")).longValue();if(user.admin()||user.assignedScopes().contains(scope))return;if(scopes.canAccess(scope)){scopes.requireOperation(scope,operation);return;}if(!sharing.hasResourceAccess("DATASET",id,operation))throw BusinessException.forbidden("共享规则未授权该操作");}

    public void requireWriteAccess(Map<String,Object> dataset){UserPrincipal user=CurrentUser.require();long scope=((Number)dataset.get("dataScopeId")).longValue();if(user.admin()||user.assignedScopes().contains(scope))return;throw BusinessException.forbidden("选择性共享仅授予读取类操作，不能修改数据");}

    public boolean canAudit(long datasetId){Map<String,Object> dataset=get(datasetId);UserPrincipal user=CurrentUser.require();long scope=((Number)dataset.get("dataScopeId")).longValue();return user.admin()||(user.permissions().contains("dataset:audit")&&user.assignedScopes().contains(scope));}

    @Transactional
    public long create(DatasetBody body) {
        scopes.require(body.dataScopeId());
        governanceReferenceShared(4201);
        List<Map<String, Object>> fields = body.fieldDefinition();
        Integer templateVersion = null;
        if (body.templateId() != null) {
            TemplateRef ref = jdbc.sql("SELECT version,published,type,schema_definition,content,visibility,enabled,creator_id FROM tpl_template WHERE id=:id AND deleted=0 FOR SHARE")
                    .param("id", body.templateId()).query(TemplateRef.class).optional()
                    .orElseThrow(() -> BusinessException.notFound("引用模板不存在"));
            requireUsableTemplate(ref);
            if (!"template".equals(ref.type())) throw BusinessException.badRequest("模板片段不能直接用于创建数据集");
            templateVersion = ref.version();
            validateTemplateMatch(fields, ref);
        }
        validateFieldDefinitions(fields);
        validateCategories(body.scientificCategoryId(),body.industryCategoryId());
        UserPrincipal user = CurrentUser.require();String canonicalTags=normalizeTags(body.tags());
        Long id = jdbc.sql("""
                INSERT INTO data_dataset(name,description,category,tags,field_definition,status,creator_id,creator_name,
                                         data_scope_id,template_id,template_version,scientific_category_id,industry_category_id)
                VALUES (:name,:description,:category,:tags,CAST(:fields AS jsonb),0,:creatorId,:creatorName,
                        :scopeId,:templateId,:templateVersion,:scientificCategoryId,:industryCategoryId) RETURNING id
                """).param("name", body.name()).param("description", body.description()).param("category", body.category())
                .param("tags", canonicalTags).param("fields", json.write(fields)).param("creatorId", user.id())
                .param("creatorName", user.realName()).param("scopeId", body.dataScopeId())
                .param("templateId", body.templateId()).param("templateVersion", templateVersion)
                .param("scientificCategoryId",body.scientificCategoryId()).param("industryCategoryId",body.industryCategoryId()).query(Long.class).single();
        syncTags(id,canonicalTags);
        audit.record("CREATE", "DATASET", "创建数据集：" + body.name());
        return id;
    }

    @Transactional
    public void update(long id, DatasetBody body) {
        mutationLock.lockTransaction(id);
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        long dataCount=((Number) existing.get("dataCount")).longValue();boolean recoverableArchive=hasRecoverableArchive(id);
        ensureNoPendingCreates(id);
        if ((dataCount>0||recoverableArchive) && !existing.get("fieldDefinition").equals(body.fieldDefinition())) {
            throw BusinessException.badRequest("数据集已有记录，不能直接修改字段定义；请创建新版本数据集");
        }
        Long existingTemplate=existing.get("templateId")==null?null:((Number)existing.get("templateId")).longValue();
        if((dataCount>0||recoverableArchive)&&!java.util.Objects.equals(existingTemplate,body.templateId()))throw BusinessException.badRequest("数据集存在在线或可恢复归档记录，不能变更模板来源");
        scopes.require(body.dataScopeId());
        long oldScope=((Number)existing.get("dataScopeId")).longValue();
        if(oldScope!=body.dataScopeId()&&(dataCount>0||recoverableArchive||hasAttachments(id)))throw BusinessException.badRequest("数据集存在在线/归档记录或附件，不能直接变更数据域；请执行受控迁移");
        validateFieldDefinitions(body.fieldDefinition());
        governanceReferenceShared(4201);validateCategories(body.scientificCategoryId(),body.industryCategoryId());String canonicalTags=normalizeTags(body.tags());
        Integer templateVersion=null;
        if(body.templateId()!=null){TemplateRef ref=jdbc.sql("SELECT version,published,type,schema_definition,content,visibility,enabled,creator_id FROM tpl_template WHERE id=:id AND deleted=0 FOR SHARE")
                .param("id",body.templateId()).query(TemplateRef.class).optional().orElseThrow(()->BusinessException.notFound("引用模板不存在"));
            requireUsableTemplate(ref);
            if(!"template".equals(ref.type()))throw BusinessException.badRequest("模板片段不能直接用于更新数据集");
            validateTemplateMatch(body.fieldDefinition(),ref);templateVersion=ref.version();}
        jdbc.sql("""
                UPDATE data_dataset SET name=:name,description=:description,category=:category,tags=:tags,
                field_definition=CAST(:fields AS jsonb),data_scope_id=:scopeId,template_id=:templateId,
                template_version=:templateVersion,scientific_category_id=:scientificCategoryId,
                industry_category_id=:industryCategoryId,version=version+1,updated_time=now()
                WHERE id=:id
                """).param("name", body.name()).param("description", body.description()).param("category", body.category())
                .param("tags", canonicalTags).param("fields", json.write(body.fieldDefinition()))
                .param("scopeId", body.dataScopeId()).param("templateId",body.templateId()).param("templateVersion",templateVersion)
                .param("scientificCategoryId",body.scientificCategoryId()).param("industryCategoryId",body.industryCategoryId()).param("id", id).update();
        syncTags(id,canonicalTags);
        audit.record("UPDATE", "DATASET", "更新数据集：" + id,
                Map.of("datasetId",id,"before",auditSnapshot(existing),"after",auditSnapshot(get(id))));
    }

    @Transactional
    public void delete(long id) {
        mutationLock.lockTransaction(id);
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        ensureNoPendingCreates(id);
        if (((Number) existing.get("dataCount")).longValue() > 0||hasRecoverableArchive(id)||hasAttachments(id)) {
            throw BusinessException.badRequest("数据集存在在线记录、可恢复归档或附件，不能删除");
        }
        jdbc.sql("UPDATE data_dataset SET deleted=1,updated_time=now() WHERE id=:id").param("id", id).update();
        Map<String,Object> deleted=new LinkedHashMap<>(auditSnapshot(existing));deleted.put("deleted",true);
        audit.record("DELETE", "DATASET", "逻辑删除数据集：" + id,Map.of("datasetId",id,"before",auditSnapshot(existing),"after",deleted));
    }

    private boolean hasRecoverableArchive(long datasetId){return jdbc.sql("SELECT count(*) FROM lifecycle_archive_item WHERE dataset_id=:dataset AND (state IN ('ARCHIVING','ARCHIVED','RESTORING') OR (state='FAILED' AND archive_path IS NOT NULL))").param("dataset",datasetId).query(Long.class).single()>0;}

    @Transactional
    public void favorite(long id) {
        get(id);
        jdbc.sql("""
                INSERT INTO user_favorite(user_id,target_type,target_id) VALUES (:userId,'dataset',:id)
                ON CONFLICT(user_id,target_type,target_id) DO NOTHING
                """).param("userId",CurrentUser.require().id()).param("id",id).update();
        audit.record("FAVORITE","DATASET","收藏数据集",Map.of("datasetId",id));
    }

    @Transactional
    public void unfavorite(long id) {
        jdbc.sql("DELETE FROM user_favorite WHERE user_id=:userId AND target_type='dataset' AND target_id=:id")
                .param("userId",CurrentUser.require().id()).param("id",id).update();
        audit.record("UNFAVORITE","DATASET","取消收藏数据集",Map.of("datasetId",id));
    }

    public PageResponse<Map<String, Object>> records(long datasetId, int pageNum, int pageSize) {
        get(datasetId);
        int page = Math.max(1, pageNum), size = Math.max(1, Math.min(pageSize, 200));
        UserPrincipal user=CurrentUser.require();boolean includeUnpublished=canAudit(datasetId);
        Query base = Query.query(visibleRecordsCriteria(user.id(),includeUnpublished));
        long total = includeUnpublished?mongo.count(base,collection(datasetId)):authoritativeVisibleCount(datasetId,user.id());
        Query query = Query.query(visibleRecordsCriteria(user.id(),includeUnpublished)).with(Sort.by(Sort.Direction.DESC, "createdTime"))
                .skip((long) (page - 1) * size).limit(size);
        List<Map<String, Object>> values = new ArrayList<>(mongo.find(query, Document.class, collection(datasetId)).stream().map(this::recordView).map(LinkedHashMap::new).map(v->(Map<String,Object>)v).toList());
        if(!includeUnpublished)filterAuthoritativeVisibility(datasetId,values,user.id());enrichWorkflow(datasetId,values);
        return PageResponse.of(total, page, size, values);
    }

    public Map<String,Object> record(long datasetId,String recordId){
        get(datasetId);UserPrincipal user=CurrentUser.require();boolean includeUnpublished=canAudit(datasetId);
        Document document=mongo.findOne(Query.query(Criteria.where("_id").is(objectId(recordId)).and("deleted").ne(true).and("archived").ne(true)),Document.class,collection(datasetId));
        if(document==null||!authoritativelyVisible(datasetId,document,user.id(),includeUnpublished))throw BusinessException.notFound("记录不存在");
        Map<String,Object> value=new LinkedHashMap<>(recordView(document));enrichWorkflow(datasetId,List.of(value));return value;
    }

    public Map<String, Object> createRecord(long datasetId, UUID idempotencyKey, Map<String, Object> data) {
        return mutationLock.withSessionLock(datasetId,()->createRecordLocked(datasetId,idempotencyKey,data));
    }

    private Map<String, Object> createRecordLocked(long datasetId, UUID idempotencyKey, Map<String, Object> data) {
        Map<String, Object> dataset = get(datasetId);
        requireWriteAccess(dataset);
        validateRecord(fields(dataset), data);
        UserPrincipal user = CurrentUser.require();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("datasetId", datasetId); details.put("after", data); details.put("reason", "新增记录");
        CrossStoreOutboxService.Event event = outbox.prepare(idempotencyKey, datasetId, null, "CREATE", details,
                Map.of("data", data));
        Document document = new Document("datasetId", datasetId)
                .append("dataScopeId", ((Number) dataset.get("dataScopeId")).longValue())
                .append("data", Document.parse(json.write(data))).append("version", 1)
                .append("createdBy", user.id()).append("createdByName", user.realName())
                .append("createdTime", Instant.now()).append("deleted", false).append("workflowStatus","DRAFT")
                .append("outboxEventId", event.id()).append("outboxEventKey", event.eventKey().toString());
        try {
            ensureEventIndex(datasetId);
            Document existing = mongo.findOne(Query.query(Criteria.where("outboxEventKey").is(event.eventKey().toString())),
                    Document.class, collection(datasetId));
            if (existing != null) {
                String recordId=existing.getObjectId("_id").toHexString();outbox.completeCreate(event, datasetId,recordId, outbox.details(event));
                Map<String,Object> view=new LinkedHashMap<>(recordView(existing));view.put("workflowStatus","DRAFT");return view;
            }
            Document inserted = mongo.insert(document, collection(datasetId));
            String recordId=inserted.getObjectId("_id").toHexString();outbox.completeCreate(event, datasetId,recordId, outbox.details(event));
            Map<String,Object> view=new LinkedHashMap<>(recordView(inserted));view.put("workflowStatus","DRAFT");return view;
        } catch (Exception ex) {
            outbox.fail(event.id(), ex);
            throw new BusinessException(503, "记录暂未写入，已保留跨库补偿事件，请稍后重试");
        }
    }

    public Map<String, Object> updateRecord(long datasetId, String recordId, int expectedVersion, String reason,
                                             UUID idempotencyKey, Map<String, Object> data) {
        Map<String, Object> dataset = get(datasetId);
        requireWriteAccess(dataset);
        validateRecord(fields(dataset), data);
        Document beforeDoc = mongo.findOne(Query.query(Criteria.where("_id").is(objectId(recordId)).and("deleted").ne(true)),
                Document.class, collection(datasetId));
        if (beforeDoc == null) throw BusinessException.notFound("记录不存在");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("datasetId", datasetId); details.put("recordId", recordId);
        details.put("before", beforeDoc.get("data")); details.put("after", data); details.put("reason", reason);
        details.put("expectedVersion", expectedVersion);
        CrossStoreOutboxService.Event event = outbox.prepareCorrection(idempotencyKey, datasetId, recordId, details,
                Map.of("expectedVersion", expectedVersion, "reason", reason, "data", data),expectedVersion);
        if (event.eventKey().toString().equals(beforeDoc.getString("lastOutboxEventKey"))) {
            outbox.completeUpdate(event, outbox.details(event));
            return recordView(beforeDoc);
        }
        if (beforeDoc.getInteger("version", 1) != expectedVersion) {
            BusinessException conflict = new BusinessException(409, "记录已被其他用户更新，请刷新后重试");
            outbox.rejectCorrection(event, conflict); throw conflict;
        }
        Query query = Query.query(Criteria.where("_id").is(objectId(recordId)).and("deleted").ne(true).and("version").is(expectedVersion));
        Update update = new Update().set("data", Document.parse(json.write(data))).set("updatedBy", CurrentUser.require().id())
                .set("updatedTime", Instant.now()).set("correctionReason", reason)
                .set("workflowStatus","DRAFT")
                .set("lastOutboxEventKey", event.eventKey().toString()).inc("version", 1);
        Document updated = mongo.findAndModify(query, update, org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                Document.class, collection(datasetId));
        if (updated == null) {
            Document sameRequest = mongo.findOne(Query.query(Criteria.where("_id").is(objectId(recordId))
                            .and("deleted").ne(true).and("lastOutboxEventKey").is(event.eventKey().toString())
                            .and("version").is(expectedVersion + 1)),
                    Document.class, collection(datasetId));
            if (sameRequest != null) {
                outbox.completeUpdate(event, outbox.details(event));
                return recordView(sameRequest);
            }
            BusinessException conflict = new BusinessException(409, "记录不存在或已被其他用户更新，请刷新后重试");
            outbox.rejectCorrection(event, conflict); throw conflict;
        }
        try { outbox.completeUpdate(event, outbox.details(event)); }
        catch (Exception ex) { outbox.fail(event.id(), ex); throw new BusinessException(503, "更正已写入，审计补偿处理中，请使用同一幂等键查询结果"); }
        return recordView(updated);
    }

    public void deleteRecord(long datasetId, String recordId, int expectedVersion, String reason, UUID idempotencyKey) {
        Map<String,Object> dataset=get(datasetId);requireWriteAccess(dataset);
        Document beforeDoc = mongo.findOne(Query.query(Criteria.where("_id").is(objectId(recordId))), Document.class, collection(datasetId));
        if (beforeDoc == null) throw BusinessException.notFound("记录不存在");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("datasetId", datasetId); details.put("recordId", recordId);
        details.put("before", beforeDoc.get("data")); details.put("after", Map.of("deleted", true)); details.put("reason", reason);
        details.put("expectedVersion", expectedVersion);
        if(idempotencyKey.toString().equals(beforeDoc.getString("lastOutboxEventKey"))&&beforeDoc.getBoolean("deleted",false)){CrossStoreOutboxService.Event completed=outbox.prepareDelete(idempotencyKey,datasetId,recordId,details,Map.of("expectedVersion",expectedVersion,"reason",reason),expectedVersion);outbox.completeDelete(completed,datasetId,outbox.details(completed));return;}
        if (beforeDoc.getInteger("version", 1) != expectedVersion || beforeDoc.getBoolean("deleted", false)) {
            throw new BusinessException(409, "记录已删除或版本已变化");
        }
        CrossStoreOutboxService.Event event = outbox.prepareDelete(idempotencyKey, datasetId, recordId, details,
                Map.of("expectedVersion", expectedVersion, "reason", reason),expectedVersion);
        Query query = Query.query(Criteria.where("_id").is(objectId(recordId)).and("deleted").ne(true).and("version").is(expectedVersion));
        UpdateResult result = mongo.updateFirst(query, new Update().set("deleted", true).set("deletedBy", CurrentUser.require().id())
                .set("deletedTime", Instant.now()).set("deletionReason", reason)
                .set("workflowStatus","DELETED")
                .set("lastOutboxEventKey", event.eventKey().toString()).inc("version", 1), collection(datasetId));
        if (result.getModifiedCount() == 0) {
            Document sameRequest = mongo.findOne(Query.query(Criteria.where("_id").is(objectId(recordId))
                            .and("deleted").is(true).and("lastOutboxEventKey").is(event.eventKey().toString())
                            .and("version").is(expectedVersion + 1)),
                    Document.class, collection(datasetId));
            if (sameRequest != null) {
                outbox.completeDelete(event, datasetId, outbox.details(event));
                return;
            }
            BusinessException conflict = new BusinessException(409, "记录不存在或版本已变化");
            outbox.rejectDelete(event, conflict); throw conflict;
        }
        try { outbox.completeDelete(event, datasetId, outbox.details(event)); }
        catch (Exception ex) { outbox.fail(event.id(), ex); throw new BusinessException(503, "删除已写入，审计补偿处理中，请使用同一幂等键查询结果"); }
    }

    public void exportCsv(long datasetId, Writer writer) throws IOException {
        Map<String, Object> dataset = get(datasetId);
        List<Map<String, Object>> fields = fields(dataset);
        writer.write("\uFEFF");
        writer.write(fields.stream().map(field -> escape(String.valueOf(field.get("label")))).reduce((a,b) -> a + "," + b).orElse(""));
        writer.write('\n');
        Query query = Query.query(Criteria.where("deleted").ne(true)).with(Sort.by(Sort.Direction.ASC, "createdTime"));
        try (Stream<Document> stream = mongo.stream(query, Document.class, collection(datasetId))) {
            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                Document data = iterator.next().get("data", Document.class);
                List<String> values = new ArrayList<>();
                for (Map<String, Object> field : fields) values.add(escape(csvValue(data.get(field.get("key")))));
                writer.write(String.join(",", values)); writer.write('\n');
            }
        }
        audit.record("EXPORT", "DATASET", "导出数据集 " + datasetId + " CSV");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fields(Map<String, Object> dataset) {
        return (List<Map<String, Object>>) dataset.get("fieldDefinition");
    }

    private void validateFieldDefinitions(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty()) throw BusinessException.badRequest("字段定义不能为空");
        if (fields.size() > 200) throw BusinessException.badRequest("字段数量不能超过200个");
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Map<String, Object> field : fields) {
            String key = String.valueOf(field.getOrDefault("key", ""));
            String label = String.valueOf(field.getOrDefault("label", ""));
            String type = normalizeType(String.valueOf(field.getOrDefault("type", "")));
            if (!key.matches("^[A-Za-z][A-Za-z0-9_]{0,63}$") || label.isBlank()) throw BusinessException.badRequest("字段标识或名称不正确");
            if (!keys.add(key)) throw BusinessException.badRequest("字段标识重复：" + key);
            if (!List.of("string","number","date","boolean","file","object","array").contains(type)) throw BusinessException.badRequest("不支持的字段类型：" + type);
            if(field.get("pattern")!=null){String pattern=String.valueOf(field.get("pattern"));if(pattern.length()>200||pattern.matches(".*(\\([^)]*[+*][^)]*\\)[+*]|\\\\[1-9]).*"))throw BusinessException.badRequest("字段正则过长或包含高风险回溯结构");try{java.util.regex.Pattern.compile(pattern);}catch(Exception ex){throw BusinessException.badRequest("字段正则无效："+key);}}
            int min=intConstraint(field,"minLength",0),max=intConstraint(field,"maxLength",1000000);if(min<0||max<min||max>1000000)throw BusinessException.badRequest("字段长度约束不正确："+key);
            java.math.BigDecimal minimum=decimalConstraint(field,"min"),maximum=decimalConstraint(field,"max");
            if(minimum!=null&&maximum!=null&&minimum.compareTo(maximum)>0)throw BusinessException.badRequest("字段数值范围不正确："+key);
            if(field.get("maskType")!=null&&!Set.of("PHONE","EMAIL","ID_CARD","NAME","GENERIC").contains(String.valueOf(field.get("maskType")).toUpperCase(java.util.Locale.ROOT)))throw BusinessException.badRequest("字段脱敏类型不正确："+key);
        }
    }

    public long export(long datasetId,Map<String,Object> dataset,String format,List<String> requestedFields,OutputStream output,long actorId,String actorUsername,boolean includeUnpublished)throws IOException{
        return exportInternal(datasetId,dataset,format,requestedFields,output,actorId,actorUsername,includeUnpublished,true);
    }
    long exportForPackage(long datasetId,Map<String,Object> dataset,String format,List<String> requestedFields,OutputStream output,long actorId,String actorUsername,boolean includeUnpublished)throws IOException{
        return exportInternal(datasetId,dataset,format,requestedFields,output,actorId,actorUsername,includeUnpublished,false);
    }
    private long exportInternal(long datasetId,Map<String,Object> dataset,String format,List<String> requestedFields,OutputStream output,long actorId,String actorUsername,boolean includeUnpublished,boolean auditAttempt)throws IOException{
        List<Map<String,Object>> available=fields(dataset);List<Map<String,Object>> selected=selectFields(available,requestedFields);
        String normalized=format==null?"csv":format.toLowerCase(java.util.Locale.ROOT);CountingOutputStream counted=new CountingOutputStream(output);MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(Exception ex){throw new IllegalStateException(ex);}DigestOutputStream signed=new DigestOutputStream(counted,digest);
        try{long count=switch(normalized){case "csv"->writeCsv(datasetId,selected,signed,actorId,includeUnpublished);case "json"->writeJson(datasetId,selected,signed,actorId,includeUnpublished);case "xlsx"->writeXlsx(datasetId,selected,signed,actorId,includeUnpublished);default->throw BusinessException.badRequest("导出格式仅支持 csv、json、xlsx");};signed.flush();if(auditAttempt)audit.recordAs(actorId,actorUsername,"EXPORT","DATASET","导出数据集",Map.of("datasetId",datasetId,"format",normalized,"fields",selected.stream().map(f->f.get("key")).toList(),"maskedFields",selected.stream().filter(this::sensitive).map(f->f.get("key")).toList(),"recordCount",count,"sizeBytes",counted.count(),"sha256",HexFormat.of().formatHex(digest.digest()),"result","COMPLETED"));return count;
        }catch(Exception ex){if(auditAttempt)audit.recordAs(actorId,actorUsername,"EXPORT_FAILED","DATASET","导出数据集失败",Map.of("datasetId",datasetId,"format",normalized,"fields",selected.stream().map(f->f.get("key")).toList(),"sizeBytes",counted.count(),"errorType",ex.getClass().getSimpleName(),"result","FAILED"));if(ex instanceof IOException io)throw io;if(ex instanceof RuntimeException runtime)throw runtime;throw new IOException(ex);}
    }

    private long writeCsv(long datasetId,List<Map<String,Object>> selected,OutputStream output,long actorId,boolean includeUnpublished)throws IOException{BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));writer.write("\uFEFF");writer.write(selected.stream().map(field->escape(csvValue(field.get("label")))).reduce((a,b)->a+","+b).orElse(""));writer.write('\n');long count=0;Query query=exportQuery(actorId,includeUnpublished);try(Stream<Document> stream=mongo.stream(query,Document.class,collection(datasetId))){var iterator=stream.iterator();while(iterator.hasNext()){Document document=iterator.next();if(!authoritativelyVisible(datasetId,document,actorId,includeUnpublished))continue;Document data=document.get("data",Document.class);List<String> values=new ArrayList<>();for(Map<String,Object> field:selected)values.add(escape(csvValue(exportValue(field,data.get(field.get("key"))))));writer.write(String.join(",",values));writer.write('\n');count++;}}writer.flush();return count;}
    private long writeJson(long datasetId,List<Map<String,Object>> selected,OutputStream output,long actorId,boolean includeUnpublished)throws IOException{BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));writer.write('[');boolean first=true;long count=0;Query query=exportQuery(actorId,includeUnpublished);try(Stream<Document> stream=mongo.stream(query,Document.class,collection(datasetId))){var iterator=stream.iterator();while(iterator.hasNext()){Document document=iterator.next();if(!authoritativelyVisible(datasetId,document,actorId,includeUnpublished))continue;Document data=document.get("data",Document.class);Map<String,Object> value=new LinkedHashMap<>();for(Map<String,Object> field:selected)value.put(String.valueOf(field.get("key")),exportValue(field,data.get(field.get("key"))));if(!first)writer.write(',');writer.write(json.write(value));first=false;count++;}}writer.write(']');writer.flush();return count;}
    private long writeXlsx(long datasetId,List<Map<String,Object>> selected,OutputStream output,long actorId,boolean includeUnpublished)throws IOException{org.apache.poi.xssf.streaming.SXSSFWorkbook workbook=new org.apache.poi.xssf.streaming.SXSSFWorkbook(100);workbook.setCompressTempFiles(true);long count=0;try{var sheet=workbook.createSheet("data");var header=sheet.createRow(0);for(int i=0;i<selected.size();i++)header.createCell(i).setCellValue(String.valueOf(selected.get(i).get("label")));Query query=exportQuery(actorId,includeUnpublished);try(Stream<Document> stream=mongo.stream(query,Document.class,collection(datasetId))){var iterator=stream.iterator();while(iterator.hasNext()){Document document=iterator.next();if(!authoritativelyVisible(datasetId,document,actorId,includeUnpublished))continue;Document data=document.get("data",Document.class);var row=sheet.createRow((int)count+1);for(int i=0;i<selected.size();i++){Map<String,Object> field=selected.get(i);Object value=exportValue(field,data.get(field.get("key")));var cell=row.createCell(i);if(!sensitive(field)&&value instanceof Number n)cell.setCellValue(n.doubleValue());else if(!sensitive(field)&&value instanceof Boolean b)cell.setCellValue(b);else cell.setCellValue(csvValue(value));}count++;}}workbook.write(output);output.flush();return count;}finally{workbook.dispose();workbook.close();}}
    private Query exportQuery(long actorId,boolean includeUnpublished){return Query.query(visibleRecordsCriteria(actorId,includeUnpublished)).with(Sort.by(Sort.Direction.ASC,"createdTime"));}
    private Criteria visibleRecordsCriteria(long actorId,boolean includeUnpublished){Criteria active=new Criteria().andOperator(Criteria.where("deleted").ne(true),Criteria.where("archived").ne(true));if(includeUnpublished)return active;return new Criteria().andOperator(active,new Criteria().orOperator(Criteria.where("workflowStatus").is("PUBLISHED"),Criteria.where("createdBy").is(actorId)));}
    private boolean authoritativelyVisible(long datasetId,Document document,long actorId,boolean includeUnpublished){if(includeUnpublished||((Number)document.get("createdBy")).longValue()==actorId)return true;String recordId=document.getObjectId("_id").toHexString();return jdbc.sql("SELECT count(*) FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record AND status='PUBLISHED'").param("dataset",datasetId).param("record",recordId).query(Long.class).single()>0;}
    private void filterAuthoritativeVisibility(long datasetId,List<Map<String,Object>> values,long actorId){List<String> candidates=values.stream().filter(v->((Number)v.get("createdBy")).longValue()!=actorId).map(v->String.valueOf(v.get("id"))).toList();if(candidates.isEmpty())return;java.util.Set<String> published=new java.util.HashSet<>(jdbc.sql("SELECT record_id FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id IN (:records) AND status='PUBLISHED'").param("dataset",datasetId).param("records",candidates).query(String.class).list());values.removeIf(v->((Number)v.get("createdBy")).longValue()!=actorId&&!published.contains(String.valueOf(v.get("id"))));}
    private long authoritativeVisibleCount(long datasetId,long actorId){long published=jdbc.sql("SELECT count(*) FROM dataset_record_workflow WHERE dataset_id=:dataset AND status='PUBLISHED'").param("dataset",datasetId).query(Long.class).single();long owned=mongo.count(Query.query(new Criteria().andOperator(Criteria.where("deleted").ne(true),Criteria.where("archived").ne(true),Criteria.where("createdBy").is(actorId))),collection(datasetId));long ownedPublished=jdbc.sql("SELECT count(*) FROM dataset_record_workflow WHERE dataset_id=:dataset AND owner_id=:owner AND status='PUBLISHED'").param("dataset",datasetId).param("owner",actorId).query(Long.class).single();return Math.max(0,published+owned-ownedPublished);}
    private List<Map<String,Object>> selectFields(List<Map<String,Object>> available,List<String> requested){if(requested==null||requested.isEmpty())return available;java.util.Set<String> keys=new java.util.LinkedHashSet<>();for(String value:requested)for(String part:value.split(","))if(!part.isBlank())keys.add(part.trim());Map<String,Map<String,Object>> index=new LinkedHashMap<>();for(Map<String,Object> field:available)index.put(String.valueOf(field.get("key")),field);if(!index.keySet().containsAll(keys))throw BusinessException.badRequest("导出字段包含未知字段");return keys.stream().map(index::get).toList();}
    public void validateExportFields(Map<String,Object> dataset,List<String> requested){selectFields(fields(dataset),requested);}
    public void auditExportPreflightFailure(long datasetId,String format,List<String> requested,UserPrincipal actor,Exception error){Map<String,Object> details=new LinkedHashMap<>();details.put("datasetId",datasetId);details.put("format",format);details.put("fields",requested==null?List.of():requested);details.put("stage","PREFLIGHT");details.put("errorType",error.getClass().getSimpleName());details.put("result","FAILED");audit.recordAs(actor.id(),actor.username(),"EXPORT_FAILED","DATASET","导出请求预检失败",details);}
    public List<Map<String,Object>> exportFieldDescriptors(Map<String,Object> dataset){List<Map<String,Object>> result=new ArrayList<>();for(Map<String,Object> field:fields(dataset)){Map<String,Object> value=new LinkedHashMap<>();value.put("label",field.get("label"));value.put("value",field.get("key"));value.put("sensitive",sensitive(field));value.put("maskType",maskType(field));result.add(value);}return result;}

    private void validateRecord(List<Map<String, Object>> fields, Map<String, Object> data) {
        if (data == null) throw BusinessException.badRequest("记录数据不能为空");
        if (json.write(data).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1024 * 1024)
            throw BusinessException.badRequest("单条记录不能超过1MB");
        java.util.Set<String> known = fields.stream().map(f -> String.valueOf(f.get("key"))).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> unknown = new java.util.HashSet<>(data.keySet()); unknown.removeAll(known);
        if (!unknown.isEmpty()) throw BusinessException.badRequest("包含未定义字段：" + String.join(",", unknown));
        for (Map<String, Object> field : fields) {
            String key = field.get("key").toString();
            Object value = data.get(key);
            if (Boolean.TRUE.equals(field.get("required")) && (value == null || value instanceof String s && s.isBlank())) {
                throw BusinessException.badRequest(field.get("label") + "为必填项");
            }
            if (value == null) continue;
            String label=String.valueOf(field.get("label")); String type=normalizeType(String.valueOf(field.get("type")));
            boolean valid=switch(type){
                case "string","file","date" -> value instanceof String;
                case "number" -> value instanceof Number;
                case "boolean" -> value instanceof Boolean;
                case "object" -> value instanceof Map;
                case "array" -> value instanceof java.util.Collection;
                default -> false;
            };
            if(!valid) throw BusinessException.badRequest(label+"的类型应为"+type);
            if(value instanceof String s){
                int min=intConstraint(field,"minLength",0),max=intConstraint(field,"maxLength",1000000);
                if(s.length()<min||s.length()>max) throw BusinessException.badRequest(label+"长度不符合约束");
                Object pattern=field.get("pattern"); if(pattern!=null&&!s.matches(String.valueOf(pattern))) throw BusinessException.badRequest(label+"格式不正确");
                if("date".equals(type)) try{java.time.temporal.TemporalAccessor parsed=s.length()==10?java.time.LocalDate.parse(s):java.time.OffsetDateTime.parse(s);}
                    catch(Exception ex){throw BusinessException.badRequest(label+"须为ISO日期或时间");}
            }
            if(value instanceof Number n){
                java.math.BigDecimal number=new java.math.BigDecimal(n.toString());
                java.math.BigDecimal minimum=decimalConstraint(field,"min"),maximum=decimalConstraint(field,"max");
                if(minimum!=null&&number.compareTo(minimum)<0) throw BusinessException.badRequest(label+"小于最小值");
                if(maximum!=null&&number.compareTo(maximum)>0) throw BusinessException.badRequest(label+"大于最大值");
            }
            if(field.get("options") instanceof java.util.Collection<?> options&&!options.contains(value)) throw BusinessException.badRequest(label+"不在允许选项中");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateTemplateMatch(List<Map<String,Object>> supplied,TemplateRef ref){
        Map<String,Object> content=json.map(ref.content()); Object sections=content.get("sections");
        if(!(sections instanceof List<?> list)) return;
        List<Map<String,Object>> expected=new ArrayList<>();
        for(Object section:list) if(section instanceof Map<?,?> m&&m.get("fields") instanceof List<?> fs)
            for(Object f:fs) if(f instanceof Map<?,?> fm) expected.add((Map<String,Object>)fm);
        if(expected.isEmpty()) return;
        Map<String,Object> schema=ref.schemaDefinition()==null?Map.of():json.map(ref.schemaDefinition());
        Map<String,Object> properties=schema.get("properties") instanceof Map<?,?> map?(Map<String,Object>)map:Map.of();
        Map<String,com.fasterxml.jackson.databind.JsonNode> a=new java.util.TreeMap<>(),b=new java.util.TreeMap<>();
        for(Map<String,Object> f:expected){Map<String,Object> enriched=enrichSchema(f,properties.get(String.valueOf(f.get("key"))));a.put(String.valueOf(f.get("key")),normalizedField(enriched));}
        for(Map<String,Object> f:supplied){String key=String.valueOf(f.get("key"));Map<String,Object> enriched=enrichSchema(f,properties.get(key));f.clear();f.putAll(enriched);b.put(key,normalizedField(enriched));}
        if(!a.equals(b)) throw BusinessException.badRequest("字段定义必须与所选模板版本一致");
    }

    private void requireUsableTemplate(TemplateRef ref) {
        if (!ref.enabled()) throw BusinessException.badRequest("停用模板不能用于创建或更新数据集");
        if ("PUBLIC".equals(ref.visibility())) {
            if (!ref.published()) throw BusinessException.badRequest("公共模板必须审核发布后才能使用");
            return;
        }
        if (!"PRIVATE".equals(ref.visibility()) || ref.creatorId() != CurrentUser.require().id()) {
            throw BusinessException.forbidden("私有模板仅创建者本人可以使用");
        }
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> enrichSchema(Map<String,Object> field,Object rawProperty){Map<String,Object> copy=new LinkedHashMap<>(field);if(!(rawProperty instanceof Map<?,?> raw))return copy;Map<String,Object> property=new LinkedHashMap<>((Map<String,Object>)raw);Object suppliedSchema=copy.get("schema");if(suppliedSchema!=null&&!json.mapper().valueToTree(suppliedSchema).equals(json.mapper().valueToTree(property)))return copy;copy.put("schema",property);if(copy.get("min")==null&&property.get("minimum")!=null)copy.put("min",property.get("minimum"));if(copy.get("max")==null&&property.get("maximum")!=null)copy.put("max",property.get("maximum"));if(copy.get("minLength")==null&&property.get("minLength")!=null)copy.put("minLength",property.get("minLength"));if(copy.get("maxLength")==null&&property.get("maxLength")!=null)copy.put("maxLength",property.get("maxLength"));if(copy.get("pattern")==null&&property.get("pattern")!=null)copy.put("pattern",property.get("pattern"));if(copy.get("options")==null&&property.get("enum") instanceof java.util.Collection<?>)copy.put("options",property.get("enum"));return copy;}
    private com.fasterxml.jackson.databind.JsonNode normalizedField(Map<String,Object> field){Map<String,Object> copy=new LinkedHashMap<>(field);copy.put("type",normalizeType(String.valueOf(copy.get("type"))));return json.mapper().valueToTree(copy);}
    private int intConstraint(Map<String,Object> field,String key,int fallback){try{return field.get(key)==null?fallback:Integer.parseInt(field.get(key).toString());}catch(Exception ex){throw BusinessException.badRequest(key+"约束不正确");}}
    private java.math.BigDecimal decimalConstraint(Map<String,Object> field,String key){if(field.get(key)==null||String.valueOf(field.get(key)).isBlank())return null;try{return new java.math.BigDecimal(String.valueOf(field.get(key)));}catch(Exception ex){throw BusinessException.badRequest(key+"数值约束不正确");}}
    private String normalizeType(String type){return "text".equals(type)?"string":type;}

    private void requireOwnerOrAdmin(Map<String, Object> dataset) {
        UserPrincipal user = CurrentUser.require();
        if (!user.admin() && ((Number) dataset.get("creatorId")).longValue() != user.id()) throw BusinessException.forbidden("仅创建者或管理员可执行该操作");
    }

    private Map<String,Object> auditSnapshot(Map<String,Object> source){Map<String,Object> value=new LinkedHashMap<>();
        for(String key:List.of("id","name","description","category","tags","scientificCategoryId","industryCategoryId","fieldDefinition","dataCount","status",
                "dataScopeId","version","templateId","templateVersion"))value.put(key,source.get(key));
        return value;}

    private void lockRow(long id){jdbc.sql("SELECT id FROM data_dataset WHERE id=:id AND deleted=0 FOR UPDATE").param("id",id)
            .query(Long.class).optional().orElseThrow(()->BusinessException.notFound("数据集不存在"));}
    private void ensureNoPendingCreates(long id){long count=jdbc.sql("SELECT count(*) FROM cross_store_outbox WHERE aggregate_type='DATASET_RECORD' AND aggregate_id=:id AND operation='CREATE' AND status IN ('PENDING','FAILED','PROCESSING')")
            .param("id",id).query(Long.class).single();if(count>0)throw new BusinessException(409,"数据集仍有记录写入待完成，请稍后再修改或删除");}
    private boolean hasAttachments(long id){return jdbc.sql("SELECT count(*) FROM file_asset WHERE business_type='DATASET' AND business_ref=:ref AND status='AVAILABLE'")
            .param("ref",Long.toString(id)).query(Long.class).single()>0;}

    private JdbcClient.StatementSpec params(JdbcClient.StatementSpec spec, String keyword, String category, Integer status, UserPrincipal user,java.util.Set<Long> sharedIds) {
        spec = spec.param("keyword", keyword).param("category", category).param("status", status).param("userId", user.id());
        if (!user.admin() && !user.dataScopes().isEmpty()) spec = spec.param("scopes", user.dataScopes());
        if(!user.admin()&&!sharedIds.isEmpty())spec=spec.param("sharedIds",sharedIds);
        return spec;
    }

    private Map<String, Object> row(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("name", rs.getString("name"));
        value.put("description", rs.getString("description")); value.put("category", rs.getString("category"));
        value.put("tags", rs.getString("tags")); value.put("fieldDefinition", readFields(rs.getString("field_definition")));
        value.put("scientificCategoryId",rs.getObject("scientific_category_id"));value.put("industryCategoryId",rs.getObject("industry_category_id"));
        value.put("dataCount", rs.getLong("data_count")); value.put("storageSize", rs.getLong("storage_size"));
        value.put("status", rs.getInt("status")); value.put("creatorId", rs.getLong("creator_id"));
        value.put("creatorName", rs.getString("creator_name")); value.put("dataScopeId", rs.getLong("data_scope_id"));
        value.put("importanceLevel", rs.getString("importance_level"));
        value.put("version", rs.getInt("version")); value.put("templateId", rs.getObject("template_id"));
        value.put("templateVersion", rs.getObject("template_version")); value.put("provisionStatus", rs.getString("provision_status"));
        value.put("favorited", rs.getBoolean("favorited"));
        value.put("hasAttachments", rs.getBoolean("has_attachments")); value.put("hasRecoverableArchive", rs.getBoolean("has_recoverable_archive"));
        value.put("createdTime", rs.getObject("created_time")); value.put("updatedTime", rs.getObject("updated_time"));
        return value;
    }

    private List<Map<String, Object>> readFields(String value) {
        try { return json.mapper().readValue(value, new TypeReference<>() {}); }
        catch (Exception ex) { throw new IllegalStateException("字段定义损坏", ex); }
    }

    private Map<String, Object> recordView(Document document) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", document.getObjectId("_id").toHexString());
        value.put("data", document.get("data")); value.put("version", document.getInteger("version", 1));
        value.put("createdBy", document.get("createdBy")); value.put("createdByName", document.get("createdByName"));
        value.put("createdTime", document.get("createdTime")); value.put("updatedTime", document.get("updatedTime"));
        return value;
    }

    private void enrichWorkflow(long datasetId,List<Map<String,Object>> records){if(records.isEmpty())return;List<String> ids=records.stream().map(v->String.valueOf(v.get("id"))).toList();Map<String,String> statuses=new java.util.HashMap<>();jdbc.sql("SELECT record_id,status FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id IN (:ids)").param("dataset",datasetId).param("ids",ids).query((rs,n)->{statuses.put(rs.getString("record_id"),rs.getString("status"));return 1;}).list();records.forEach(v->v.put("workflowStatus",statuses.getOrDefault(String.valueOf(v.get("id")),"DRAFT")));}
    private String collection(long datasetId) { return "dataset_data_" + datasetId; }
    private void ensureEventIndex(long datasetId) {
        mongo.indexOps(collection(datasetId)).ensureIndex(new Index().on("outboxEventKey", Sort.Direction.ASC).unique()
                .partial(PartialIndexFilter.of(Criteria.where("outboxEventKey").type(2))));
    }
    private ObjectId objectId(String id) {
        try { return new ObjectId(id); } catch (Exception ex) { throw BusinessException.badRequest("记录ID格式不正确"); }
    }
    private String csvValue(Object value) {
        String text = value == null ? "" : value instanceof String ? value.toString() : json.write(value);
        String stripped=text.stripLeading();if(!text.isEmpty()&&("\t\r\n".indexOf(text.charAt(0))>=0||!stripped.isEmpty()&&"=+-@".indexOf(stripped.charAt(0))>=0))text="'"+text;
        return text;
    }
    private Object exportValue(Map<String,Object> field,Object value){if(value==null||!sensitive(field))return value;String text=value instanceof String?String.valueOf(value):json.write(value);if(text.isBlank())return text;return switch(maskType(field)){case "PHONE"->text.length()<=7?stars(text.length()):text.substring(0,3)+stars(text.length()-7)+text.substring(text.length()-4);case "EMAIL"->{int at=text.indexOf('@');yield at<=0?genericMask(text):text.substring(0,1)+stars(Math.max(3,at-1))+text.substring(at);}case "ID_CARD"->text.length()<=8?genericMask(text):text.substring(0,4)+stars(text.length()-8)+text.substring(text.length()-4);case "NAME"->text.substring(0,1)+stars(Math.max(2,text.length()-1));default->genericMask(text);};}
    private boolean sensitive(Map<String,Object> field){if(Boolean.parseBoolean(String.valueOf(field.getOrDefault("sensitive",false))))return true;String value=(String.valueOf(field.getOrDefault("key",""))+" "+String.valueOf(field.getOrDefault("label",""))).toLowerCase(java.util.Locale.ROOT);return value.matches(".*(phone|mobile|email|idcard|id_number|身份证|手机号|电话|邮箱).*?");}
    private String maskType(Map<String,Object> field){String explicit=String.valueOf(field.getOrDefault("maskType","")).toUpperCase(java.util.Locale.ROOT);if(Set.of("PHONE","EMAIL","ID_CARD","NAME","GENERIC").contains(explicit))return explicit;String value=(String.valueOf(field.getOrDefault("key",""))+" "+String.valueOf(field.getOrDefault("label",""))).toLowerCase(java.util.Locale.ROOT);if(value.matches(".*(phone|mobile|手机号|电话).*") )return "PHONE";if(value.matches(".*(email|邮箱).*") )return "EMAIL";if(value.matches(".*(idcard|id_number|身份证).*") )return "ID_CARD";return "GENERIC";}
    private String genericMask(String text){if(text.length()<=2)return stars(Math.max(3,text.length()));return text.substring(0,1)+stars(Math.max(3,text.length()-2))+text.substring(text.length()-1);}
    private String stars(int count){return "*".repeat(Math.max(0,Math.min(count,64)));}
    private String escape(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }

    private void syncTags(long datasetId,String raw){
        governanceReferenceShared(4202);
        jdbc.sql("DELETE FROM data_dataset_tag WHERE dataset_id=:id").param("id",datasetId).update();
        java.util.LinkedHashSet<String> names=new java.util.LinkedHashSet<>();
        if(raw!=null)for(String value:raw.split(",")){String name=value.trim();if(!name.isBlank())names.add(name.toLowerCase(java.util.Locale.ROOT));}
        for(String name:names){
            jdbc.sql("INSERT INTO gov_tag(tag_name,tag_group,created_by) VALUES (:name,'业务录入',:actor) ON CONFLICT DO NOTHING").param("name",name).param("actor",CurrentUser.require().id()).update();
            Long tagId=jdbc.sql("SELECT id FROM gov_tag WHERE lower(tag_name)=:name AND status=1 AND deleted=0 FOR SHARE").param("name",name).query(Long.class).optional().orElseThrow(()->BusinessException.badRequest("标签已停用或不可用："+name));
            jdbc.sql("INSERT INTO data_dataset_tag(dataset_id,tag_id) VALUES (:dataset,:tag) ON CONFLICT DO NOTHING").param("dataset",datasetId).param("tag",tagId).update();
        }
    }

    private String normalizeTags(String raw){java.util.LinkedHashSet<String> values=new java.util.LinkedHashSet<>();if(raw!=null)for(String value:raw.split(",")){String normalized=value.trim().toLowerCase(java.util.Locale.ROOT);if(!normalized.isBlank())values.add(normalized);}return String.join(",",values);}
    private void governanceReferenceShared(long key){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(:key)) locked").param("key",key).query(Long.class).single();}

    private void validateCategories(Long scientificId,Long industryId){
        if(scientificId!=null){long count=jdbc.sql("SELECT count(*) FROM gov_category WHERE id=:id AND category_type='SCIENTIFIC' AND status=1 AND deleted=0").param("id",scientificId).query(Long.class).single();if(count==0)throw BusinessException.badRequest("科学分类不存在或已停用");}
        if(industryId!=null){long count=jdbc.sql("SELECT count(*) FROM gov_category WHERE id=:id AND category_type='INDUSTRY' AND status=1 AND deleted=0").param("id",industryId).query(Long.class).single();if(count==0)throw BusinessException.badRequest("产业分类不存在或已停用");}
        if(scientificId!=null&&industryId!=null){long mappings=jdbc.sql("SELECT count(*) FROM gov_category_mapping WHERE scientific_category_id=:scientific AND industry_category_id=:industry").param("scientific",scientificId).param("industry",industryId).query(Long.class).single();if(mappings==0)throw BusinessException.badRequest("科学分类与产业分类尚未建立映射");}
    }

    public record DatasetBody(String name, String description, String category, String tags,Long scientificCategoryId,Long industryCategoryId,
                              List<Map<String, Object>> fieldDefinition, long dataScopeId, Long templateId) {}
    private record TemplateRef(int version, boolean published, String type, String schemaDefinition, String content,
                               String visibility, boolean enabled, long creatorId) {}
    private static final class CountingOutputStream extends FilterOutputStream {private long count;private CountingOutputStream(OutputStream out){super(out);}@Override public void write(int value)throws IOException{out.write(value);count++;}@Override public void write(byte[] buffer,int offset,int length)throws IOException{out.write(buffer,offset,length);count+=length;}long count(){return count;}}
}
