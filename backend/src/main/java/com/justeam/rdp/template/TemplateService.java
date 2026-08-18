package com.justeam.rdp.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.common.PageResponse;
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

@Service
public class TemplateService {
    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final DataScopeService scopes;
    private final AuditService audit;

    public TemplateService(JdbcClient jdbc, JsonSupport json, DataScopeService scopes, AuditService audit) {
        this.jdbc = jdbc;
        this.json = json;
        this.scopes = scopes;
        this.audit = audit;
    }

    public PageResponse<Map<String, Object>> list(String keyword, String type, Boolean published, String visibility,
                                                  Boolean enabled, Boolean usable, String view, int pageNum, int pageSize) {
        UserPrincipal user = CurrentUser.require();
        int size = Math.max(1, Math.min(pageSize, 100));
        int page = Math.max(1, pageNum);
        boolean canAudit = user.admin() || user.permissions().contains("template:audit");
        String normalizedView=normalizeListView(view);
        String scopeClause = user.admin() ? "TRUE" : user.assignedScopes().isEmpty()
                ? "(t.creator_id=:userId OR (t.visibility='PUBLIC' AND t.published=TRUE AND t.enabled=TRUE))"
                : "(t.creator_id=:userId OR (t.visibility='PUBLIC' AND t.published=TRUE AND t.enabled=TRUE)"
                + (canAudit ? " OR (t.visibility='PUBLIC' AND t.data_scope_id IN (:scopes) AND (t.audit_status=0 OR t.published=TRUE))" : "") + ")";
        String where = """
                FROM tpl_template t WHERE t.deleted = 0
                AND (CAST(:keyword AS text) IS NULL OR lower(t.name) LIKE lower(concat('%', CAST(:keyword AS text), '%')) OR lower(coalesce(t.tag,'')) LIKE lower(concat('%', CAST(:keyword AS text), '%')))
                AND (CAST(:type AS text) IS NULL OR t.type = CAST(:type AS text))
                AND (CAST(:published AS boolean) IS NULL OR t.published = CAST(:published AS boolean))
                AND (CAST(:visibility AS text) IS NULL OR t.visibility = CAST(:visibility AS text))
                AND (CAST(:enabled AS boolean) IS NULL OR t.enabled = CAST(:enabled AS boolean))
                """ + " AND " + scopeClause;
        if (Boolean.TRUE.equals(usable)) {
            where += " AND t.type='template' AND t.enabled=TRUE AND ((t.visibility='PUBLIC' AND t.published=TRUE) OR (t.visibility='PRIVATE' AND t.creator_id=:userId))";
        }
        if("MINE".equals(normalizedView))where+=" AND t.creator_id=:userId";
        else if("FAVORITES".equals(normalizedView))where+=" AND EXISTS(SELECT 1 FROM user_favorite vf WHERE vf.user_id=:userId AND vf.target_type='template' AND vf.target_id=t.id)";
        else if("REVIEW".equals(normalizedView)){
            if(!canAudit||!user.admin()&&user.assignedScopes().isEmpty())where+=" AND FALSE";
            else where+=" AND t.visibility='PUBLIC' AND t.audit_status=0 AND t.creator_id<>:userId"
                    +(user.admin()?"":" AND t.data_scope_id IN (:scopes)");
        }
        JdbcClient.StatementSpec countSpec = params(jdbc.sql("SELECT count(*) " + where), keyword, type, published,
                visibility, enabled, user, canAudit);
        long total = countSpec.query(Long.class).single();
        JdbcClient.StatementSpec listSpec = params(jdbc.sql("""
                SELECT t.*, EXISTS(SELECT 1 FROM user_favorite f WHERE f.user_id=:userId AND f.target_type='template' AND f.target_id=t.id) AS favorited
                """ + where + " ORDER BY t.created_time DESC LIMIT :limit OFFSET :offset"), keyword, type, published,
                visibility, enabled, user, canAudit)
                .param("limit", size).param("offset", (page - 1) * size);
        List<Map<String,Object>> values=listSpec.query(this::row).list().stream()
                .map(value->withAuditCapability(value,user)).toList();
        return PageResponse.of(total, page, size, values);
    }

    public Map<String, Object> get(long id) {
        long userId = CurrentUser.require().id();
        Map<String, Object> value = jdbc.sql("""
                SELECT t.*, EXISTS(SELECT 1 FROM user_favorite f WHERE f.user_id=:userId
                AND f.target_type='template' AND f.target_id=t.id) AS favorited
                FROM tpl_template t WHERE id=:id AND deleted=0
                """).param("userId", userId).param("id", id).query(this::row).optional()
                .orElseThrow(() -> BusinessException.notFound("模板不存在"));
        long scopeId = ((Number) value.get("dataScopeId")).longValue();
        boolean published = (boolean) value.get("published");
        boolean enabled = (boolean) value.get("enabled");
        String visibility = String.valueOf(value.get("visibility"));
        long creatorId = ((Number) value.get("creatorId")).longValue();
        UserPrincipal user = CurrentUser.require();
        boolean publicCatalog = "PUBLIC".equals(visibility) && published && enabled;
        boolean auditorAccess = (user.admin() || user.permissions().contains("template:audit"))
                && "PUBLIC".equals(visibility) && scopes.canWriteAccess(scopeId)
                && (published || value.get("auditStatus") instanceof Number status && status.intValue()==0);
        if (!user.admin() && creatorId != user.id() && !publicCatalog && !auditorAccess) {
            throw BusinessException.forbidden("无权访问该模板");
        }
        return withAuditCapability(value,user);
    }

    @Transactional
    public long create(TemplateBody body) {
        scopes.require(body.dataScopeId());
        UserPrincipal user = CurrentUser.require();
        validateContent(body.content(),body.schemaDefinition());
        String visibility = normalizeVisibility(body.visibility());String canonicalTags=normalizeTags(body.tag());
        Long id = jdbc.sql("""
                INSERT INTO tpl_template(name,type,tag,description,source,visibility,content,schema_definition,
                                         creator_id,creator_name,data_scope_id)
                VALUES (:name,:type,:tag,:description,:source,:visibility,CAST(:content AS jsonb),CAST(:schema AS jsonb),
                        :creatorId,:creatorName,:scopeId) RETURNING id
                """).param("name", body.name()).param("type", body.type()).param("tag", canonicalTags)
                .param("description", body.description()).param("source", body.source())
                .param("visibility", visibility)
                .param("content", json.write(body.content())).param("schema", json.write(body.schemaDefinition()))
                .param("creatorId", user.id()).param("creatorName", user.realName()).param("scopeId", body.dataScopeId())
                .query(Long.class).single();
        syncTags(id,canonicalTags);
        snapshot(id, "创建模板");
        audit.record("CREATE", "TEMPLATE", "创建模板：" + body.name());
        return id;
    }

    @Transactional
    public void update(long id, TemplateBody body) {
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        if ((boolean) existing.get("published")) throw BusinessException.badRequest("已发布模板不可直接修改，请复制后创建新版本");
        if (pending(existing)) throw BusinessException.badRequest("待审核模板已冻结，请等待审核结果后再修改");
        scopes.require(body.dataScopeId());
        long oldScope=((Number)existing.get("dataScopeId")).longValue();
        if(oldScope!=body.dataScopeId()&&hasAttachments(id))throw BusinessException.badRequest("模板已有附件，不能直接变更数据域；请先执行受控附件迁移");
        validateContent(body.content(),body.schemaDefinition());
        String visibility = normalizeVisibility(body.visibility());String canonicalTags=normalizeTags(body.tag());
        jdbc.sql("""
                UPDATE tpl_template SET name=:name,type=:type,tag=:tag,description=:description,source=:source,
                visibility=:visibility,content=CAST(:content AS jsonb),schema_definition=CAST(:schema AS jsonb),data_scope_id=:scopeId,
                version=version+1, audit_status=NULL, audit_comment=NULL, updated_time=now() WHERE id=:id
                """).param("name", body.name()).param("type", body.type()).param("tag", canonicalTags)
                .param("description", body.description()).param("source", body.source())
                .param("visibility", visibility)
                .param("content", json.write(body.content())).param("schema", json.write(body.schemaDefinition()))
                .param("scopeId", body.dataScopeId()).param("id", id).update();
        syncTags(id,canonicalTags);
        snapshot(id, "编辑模板");
        audit.record("UPDATE", "TEMPLATE", "更新模板：" + id,
                Map.of("templateId",id,"before",auditSnapshot(existing),"after",auditSnapshot(get(id))));
    }

    @Transactional
    public void delete(long id) {
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        if ((boolean) existing.get("published")) throw BusinessException.badRequest("已发布模板不可删除");
        if (pending(existing)) throw BusinessException.badRequest("待审核模板已冻结，不能删除");
        long references = jdbc.sql("SELECT count(*) FROM data_dataset WHERE deleted=0 AND template_id=:id")
                .param("id", id).query(Long.class).single();
        if (references > 0) throw BusinessException.badRequest("模板已被数据集引用，不能删除");
        jdbc.sql("UPDATE tpl_template SET deleted=1, updated_time=now() WHERE id=:id").param("id", id).update();
        Map<String,Object> deleted=new LinkedHashMap<>(auditSnapshot(existing));deleted.put("deleted",true);
        audit.record("DELETE", "TEMPLATE", "逻辑删除模板：" + id,Map.of("templateId",id,"before",auditSnapshot(existing),"after",deleted));
    }

    @Transactional
    public void publish(long id) {
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        if ((boolean) existing.get("published")) throw BusinessException.badRequest("模板已发布");
        if (pending(existing)) throw BusinessException.badRequest("模板已在待审核队列中");
        if (!"PUBLIC".equals(existing.get("visibility"))) {
            throw BusinessException.badRequest("私有模板无需审核；如需共享，请先编辑并明确改为公共模板");
        }
        jdbc.sql("UPDATE tpl_template SET enabled=TRUE,disabled_by=NULL,disabled_time=NULL,disabled_reason=NULL,audit_status=0, audit_comment=NULL, updated_time=now() WHERE id=:id")
                .param("id", id).update();
        audit.record("SUBMIT_AUDIT", "TEMPLATE", "提交模板发布审核：" + id,
                Map.of("templateId", id, "version", existing.get("version"), "visibility", "PUBLIC",
                        "before",auditSnapshot(existing),"after",auditSnapshot(get(id))));
    }

    @Transactional
    public void audit(long id, int status, String comment) {
        if (status != 1 && status != 2) throw BusinessException.badRequest("审核状态只能为通过或驳回");
        if (status == 2 && (comment == null || comment.isBlank())) throw BusinessException.badRequest("驳回时必须填写审核意见");
        UserPrincipal user = CurrentUser.require();
        lockRow(id);
        Map<String, Object> existing = get(id);
        scopes.require(((Number)existing.get("dataScopeId")).longValue());
        if (((Number) existing.get("creatorId")).longValue() == user.id()) {
            throw BusinessException.forbidden("为确保审核职责分离，创建者不能审核自己的模板");
        }
        int updated = jdbc.sql("""
                UPDATE tpl_template SET audit_status=:status,audit_comment=:comment,auditor_id=:auditorId,
                auditor_name=:auditorName,audit_time=now(),published=:published,updated_time=now()
                WHERE id=:id AND deleted=0 AND audit_status=0
                """).param("status", status).param("comment", comment).param("auditorId", user.id())
                .param("auditorName", user.realName()).param("published", status == 1).param("id", id).update();
        if (updated == 0) throw BusinessException.badRequest("模板不存在或当前状态不可审核");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("templateId", id);
        details.put("version", existing.get("version"));
        details.put("decision", status == 1 ? "APPROVE" : "REJECT");
        details.put("comment", comment);
        Map<String,Object> after=new LinkedHashMap<>(existing);after.put("auditStatus",status);after.put("auditComment",comment);after.put("published",status==1);
        details.put("before",auditSnapshot(existing));details.put("after",auditSnapshot(after));
        audit.record(status == 1 ? "APPROVE" : "REJECT", "TEMPLATE",
                (status == 1 ? "审核通过模板：" : "驳回模板：") + id, details);
    }

    @Transactional
    public void setEnabled(long id, boolean enabled, String reason) {
        if (reason == null || reason.isBlank()) throw BusinessException.badRequest("停用或启用原因不能为空");
        UserPrincipal user = CurrentUser.require();
        lockRow(id);
        Map<String, Object> existing = get(id);
        scopes.require(((Number)existing.get("dataScopeId")).longValue());
        if (!"PUBLIC".equals(existing.get("visibility")) || !(boolean) existing.get("published")) {
            throw BusinessException.badRequest("只有已审核发布的公共模板可以停用或重新启用");
        }
        if ((boolean) existing.get("enabled") == enabled) throw BusinessException.badRequest(enabled ? "模板已经启用" : "模板已经停用");
        jdbc.sql("""
                UPDATE tpl_template SET enabled=:enabled,disabled_by=:disabledBy,disabled_time=:disabledTime,
                disabled_reason=:disabledReason,updated_time=now() WHERE id=:id
                """).param("enabled", enabled).param("disabledBy", enabled ? null : user.id())
                .param("disabledTime", enabled ? null : java.sql.Timestamp.from(java.time.Instant.now()))
                .param("disabledReason", enabled ? null : reason.trim()).param("id", id).update();
        audit.record(enabled ? "ENABLE" : "DISABLE", "TEMPLATE",
                (enabled ? "重新启用模板：" : "停用模板：") + id,
                Map.of("templateId",id,"reason", reason.trim(),"before",auditSnapshot(existing),"after",auditSnapshot(get(id))));
    }

    @Transactional
    public void favorite(long id) {
        get(id);
        long userId = CurrentUser.require().id();
        jdbc.sql("""
                INSERT INTO user_favorite(user_id,target_type,target_id) VALUES (:userId,'template',:id)
                ON CONFLICT (user_id,target_type,target_id) DO NOTHING
                """)
                .param("userId", userId).param("id", id).update();
        audit.record("FAVORITE", "TEMPLATE", "收藏模板", Map.of("templateId",id));
    }

    @Transactional
    public void unfavorite(long id) {
        long userId = CurrentUser.require().id();
        jdbc.sql("DELETE FROM user_favorite WHERE user_id=:userId AND target_type='template' AND target_id=:id")
                .param("userId", userId).param("id", id).update();
        audit.record("UNFAVORITE", "TEMPLATE", "取消收藏模板", Map.of("templateId",id));
    }

    public List<Map<String, Object>> versions(long id) {
        requireVersionAccess(get(id));
        return jdbc.sql("""
                SELECT version,name,type,tag,description,source,content::text AS content,
                       schema_definition::text AS schema_definition,data_scope_id,visibility,restored_from_version,
                       change_reason,created_by,created_by_name,created_time
                FROM tpl_template_version WHERE template_id=:id ORDER BY version DESC
                """).param("id", id).query(this::versionRow).list();
    }

    public Map<String, Object> version(long id, int version) {
        requireVersionAccess(get(id));
        return jdbc.sql("""
                SELECT version,name,type,tag,description,source,content::text AS content,
                       schema_definition::text AS schema_definition,data_scope_id,visibility,restored_from_version,
                       change_reason,created_by,created_by_name,created_time
                FROM tpl_template_version WHERE template_id=:id AND version=:version
                """).param("id", id).param("version", version).query(this::versionRow).optional()
                .orElseThrow(() -> BusinessException.notFound("模板历史版本不存在"));
    }

    @Transactional
    public RestoreResult restore(long id, int version) {
        lockRow(id);
        Map<String, Object> existing = get(id);
        requireOwnerOrAdmin(existing);
        if (pending(existing)) throw BusinessException.badRequest("待审核模板不能恢复历史版本");
        Map<String, Object> source = version(id, version);
        scopes.require(((Number)source.get("dataScopeId")).longValue());
        UserPrincipal user = CurrentUser.require();
        String restoredName = String.valueOf(source.get("name")) + "（恢复自 v" + version + "）";
        if (restoredName.length() > 200) restoredName = restoredName.substring(0, 200);
        Long restoredId = jdbc.sql("""
                INSERT INTO tpl_template(name,type,tag,description,source,visibility,content,schema_definition,
                                         creator_id,creator_name,data_scope_id,parent_template_id,source_version)
                VALUES (:name,:type,:tag,:description,:source,:visibility,CAST(:content AS jsonb),CAST(:schema AS jsonb),
                        :creatorId,:creatorName,:scopeId,:parentTemplateId,:sourceVersion)
                RETURNING id
                """).param("name", restoredName).param("type", source.get("type"))
                .param("tag", source.get("tag")).param("description", source.get("description"))
                .param("source", "恢复自模板 #" + id + " v" + version).param("visibility", source.get("visibility"))
                .param("content", json.write(source.get("content"))).param("schema", json.write(source.get("schemaDefinition")))
                .param("creatorId", user.id()).param("creatorName", user.realName())
                .param("scopeId", source.get("dataScopeId")).param("parentTemplateId", id)
                .param("sourceVersion", version).query(Long.class).single();
        syncTags(restoredId,String.valueOf(source.get("tag")));
        snapshot(restoredId, "由模板 #" + id + " 的历史版本 v" + version + " 恢复", version);
        audit.record("RESTORE_VERSION", "TEMPLATE", "将模板历史版本复制为新草稿", Map.of(
                "sourceTemplateId", id, "sourceVersion", version,
                "newTemplateId", restoredId, "newVersion", 1,
                "originalPublishedUnchanged", existing.get("published")));
        return new RestoreResult(restoredId, 1, id, version);
    }

    private void snapshot(long id, String reason) { snapshot(id, reason, null); }

    private void syncTags(long templateId,String raw){
        governanceReferenceShared(4202);
        jdbc.sql("DELETE FROM tpl_template_tag WHERE template_id=:id").param("id",templateId).update();
        java.util.LinkedHashSet<String> names=new java.util.LinkedHashSet<>();
        if(raw!=null)for(String value:raw.split(",")){String name=value.trim();if(!name.isBlank())names.add(name.toLowerCase(java.util.Locale.ROOT));}
        for(String name:names){
            jdbc.sql("INSERT INTO gov_tag(tag_name,tag_group,created_by) VALUES (:name,'业务录入',:actor) ON CONFLICT DO NOTHING").param("name",name).param("actor",CurrentUser.require().id()).update();
            Long tagId=jdbc.sql("SELECT id FROM gov_tag WHERE lower(tag_name)=:name AND status=1 AND deleted=0 FOR SHARE").param("name",name).query(Long.class).optional().orElseThrow(()->BusinessException.badRequest("标签已停用或不可用："+name));
            jdbc.sql("INSERT INTO tpl_template_tag(template_id,tag_id) VALUES (:template,:tag) ON CONFLICT DO NOTHING").param("template",templateId).param("tag",tagId).update();
        }
    }

    private String normalizeTags(String raw){java.util.LinkedHashSet<String> values=new java.util.LinkedHashSet<>();if(raw!=null)for(String value:raw.split(",")){String normalized=value.trim().toLowerCase(java.util.Locale.ROOT);if(!normalized.isBlank())values.add(normalized);}return String.join(",",values);}
    private void governanceReferenceShared(long key){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(:key)) locked").param("key",key).query(Long.class).single();}

    private void snapshot(long id, String reason, Integer restoredFromVersion) {
        UserPrincipal user = CurrentUser.require();
        jdbc.sql("""
                INSERT INTO tpl_template_version(
                    template_id,version,name,type,tag,description,source,content,
                    schema_definition,data_scope_id,visibility,restored_from_version,
                    change_reason,created_by,created_by_name
                )
                SELECT id,version,name,type,tag,description,source,content,
                       schema_definition,data_scope_id,visibility,:restoredFromVersion,
                       :reason,:actor,:actorName
                FROM tpl_template WHERE id=:id
                ON CONFLICT(template_id,version) DO NOTHING
                """).param("reason", reason).param("actor", user.id()).param("actorName", user.realName())
                .param("restoredFromVersion", restoredFromVersion)
                .param("id", id).update();
    }

    private JdbcClient.StatementSpec params(JdbcClient.StatementSpec spec, String keyword, String type, Boolean published,
                                             String visibility, Boolean enabled, UserPrincipal user, boolean canAudit) {
        spec = spec.param("keyword", keyword).param("type", type).param("published", published)
                .param("visibility", visibility).param("enabled", enabled).param("userId", user.id());
        if (!user.admin() && canAudit && !user.assignedScopes().isEmpty()) spec = spec.param("scopes", user.assignedScopes());
        return spec;
    }

    private void requireOwnerOrAdmin(Map<String, Object> existing) {
        UserPrincipal user = CurrentUser.require();
        if (!user.admin() && ((Number) existing.get("creatorId")).longValue() != user.id()) {
            throw BusinessException.forbidden("仅创建者或管理员可执行该操作");
        }
    }

    private void requireVersionAccess(Map<String,Object> existing){
        UserPrincipal user=CurrentUser.require();long creator=((Number)existing.get("creatorId")).longValue();
        long scope=((Number)existing.get("dataScopeId")).longValue();
        if(user.admin()||creator==user.id()||(user.permissions().contains("template:audit")&&scopes.canWriteAccess(scope)))return;
        throw BusinessException.forbidden("模板历史版本仅创建者、管理员或本数据域审核员可见");
    }

    private boolean pending(Map<String, Object> existing) {
        return existing.get("auditStatus") instanceof Number value && value.intValue() == 0;
    }

    private String normalizeListView(String view){
        if(view==null||view.isBlank())return"ALL";
        String value=view.trim().toUpperCase(java.util.Locale.ROOT);
        if(!java.util.Set.of("ALL","REVIEW","MINE","FAVORITES").contains(value))throw BusinessException.badRequest("模板视图不正确");
        return value;
    }

    private Map<String,Object> withAuditCapability(Map<String,Object> value,UserPrincipal user){
        long creatorId=((Number)value.get("creatorId")).longValue();
        long scopeId=((Number)value.get("dataScopeId")).longValue();
        String reason=null;
        if(!pending(value))reason="模板当前不是待审核状态";
        else if(creatorId==user.id())reason="为确保职责分离，创建者不能审核自己的模板";
        else if(!user.admin()&&!user.permissions().contains("template:audit"))reason="当前账号未授予模板审核权限";
        else if(!scopes.canWriteAccess(scopeId))reason="当前账号未直接授权该模板所属数据域 #"+scopeId;
        value.put("canAudit",reason==null);
        value.put("auditRestriction",reason);
        return value;
    }

    private Map<String,Object> auditSnapshot(Map<String,Object> source){Map<String,Object> value=new LinkedHashMap<>();
        for(String key:List.of("id","name","type","tag","description","source","visibility","content","schemaDefinition",
                "dataScopeId","version","published","enabled","auditStatus","auditComment"))value.put(key,source.get(key));
        return value;}

    private void lockRow(long id){jdbc.sql("SELECT id FROM tpl_template WHERE id=:id AND deleted=0 FOR UPDATE").param("id",id)
            .query(Long.class).optional().orElseThrow(()->BusinessException.notFound("模板不存在"));}
    private boolean hasAttachments(long id){return jdbc.sql("SELECT count(*) FROM file_asset WHERE business_type='TEMPLATE' AND business_ref=:ref AND status='AVAILABLE'")
            .param("ref",Long.toString(id)).query(Long.class).single()>0;}

    private void validateContent(Map<String, Object> content,Map<String,Object> schemaDefinition) {
        if (content == null || content.isEmpty()) throw BusinessException.badRequest("模板内容不能为空");
        List<?> sections=content.get("sections") instanceof List<?> value?value:List.of(content);
        java.util.Set<String> keys=new java.util.HashSet<>();int count=0;
        for(Object section:sections){if(!(section instanceof Map<?,?> map))throw BusinessException.badRequest("模板章节格式不正确");
            Object rawFields=map.get("fields");if(!(rawFields instanceof List<?> fields))continue;
            for(Object raw:fields){if(!(raw instanceof Map<?,?> field))throw BusinessException.badRequest("模板字段格式不正确");
                String key=String.valueOf(field.get("key")),label=String.valueOf(field.get("label")),type=String.valueOf(field.get("type"));
                if(!key.matches("^[A-Za-z][A-Za-z0-9_]{0,63}$"))throw BusinessException.badRequest("字段key须为字母开头的字母、数字或下划线");
                if(!keys.add(key))throw BusinessException.badRequest("模板字段key不能重复："+key);
                if(label.isBlank()||"null".equals(label))throw BusinessException.badRequest("字段标签不能为空");
                if(!java.util.Set.of("text","string","number","date","boolean","file","object","array").contains(type))throw BusinessException.badRequest("不支持的字段类型："+type);
                if(field.get("pattern")!=null)validatePattern(String.valueOf(field.get("pattern")),key);
                java.math.BigDecimal min=decimal(field.get("min"),"字段最小值不正确："+key),max=decimal(field.get("max"),"字段最大值不正确："+key);
                if(min!=null&&max!=null&&min.compareTo(max)>0)throw BusinessException.badRequest("字段数值范围不正确："+key);
                Integer minLength=integer(field.get("minLength"),"字段最小长度不正确："+key),maxLength=integer(field.get("maxLength"),"字段最大长度不正确："+key);
                if(minLength!=null&&(minLength<0||minLength>1000000)||maxLength!=null&&(maxLength<0||maxLength>1000000)||minLength!=null&&maxLength!=null&&minLength>maxLength)
                    throw BusinessException.badRequest("字段长度约束不正确："+key);
                if(field.get("options")!=null&&!(field.get("options") instanceof java.util.Collection<?>))throw BusinessException.badRequest("字段可选值必须为数组："+key);
                count++;
            }
        }
        if(count==0)throw BusinessException.badRequest("模板至少需要一个字段");
        validateSchema(schemaDefinition,keys);
    }

    @SuppressWarnings("unchecked")
    private void validateSchema(Map<String,Object> schema,java.util.Set<String> fieldKeys){
        if(schema==null||schema.isEmpty())return;
        if(!"object".equals(String.valueOf(schema.getOrDefault("type","object"))))throw BusinessException.badRequest("模板 Schema 根类型必须为 object");
        if(schema.get("required") instanceof java.util.Collection<?> required)for(Object key:required)if(!fieldKeys.contains(String.valueOf(key)))throw BusinessException.badRequest("Schema 必填字段未在模板内容中定义："+key);
        if(!(schema.get("properties") instanceof Map<?,?> properties))return;
        for(var entry:properties.entrySet()){
            String key=String.valueOf(entry.getKey());if(!fieldKeys.contains(key))throw BusinessException.badRequest("Schema 属性未在模板内容中定义："+key);
            if(!(entry.getValue() instanceof Map<?,?> raw))throw BusinessException.badRequest("Schema 属性格式不正确："+key);
            Map<String,Object> property=(Map<String,Object>)raw;String type=String.valueOf(property.getOrDefault("type","string"));
            if(!java.util.Set.of("string","number","integer","boolean","object","array","null").contains(type))throw BusinessException.badRequest("Schema 字段类型不正确："+key);
            if(property.get("pattern")!=null)validatePattern(String.valueOf(property.get("pattern")),key);
            java.math.BigDecimal min=decimal(property.get("minimum"),"Schema minimum 不正确："+key),max=decimal(property.get("maximum"),"Schema maximum 不正确："+key);
            if(min!=null&&max!=null&&min.compareTo(max)>0)throw BusinessException.badRequest("Schema 数值范围不正确："+key);
            Integer minLength=integer(property.get("minLength"),"Schema minLength 不正确："+key),maxLength=integer(property.get("maxLength"),"Schema maxLength 不正确："+key);
            if(minLength!=null&&(minLength<0||minLength>1000000)||maxLength!=null&&(maxLength<0||maxLength>1000000)||minLength!=null&&maxLength!=null&&minLength>maxLength)
                throw BusinessException.badRequest("Schema 长度约束不正确："+key);
            if(property.get("enum")!=null&&!(property.get("enum") instanceof java.util.Collection<?>))throw BusinessException.badRequest("Schema enum 必须为数组："+key);
        }
    }
    private void validatePattern(String pattern,String key){if(pattern.length()>200||pattern.matches(".*(\\([^)]*[+*][^)]*\\)[+*]|\\\\[1-9]).*"))throw BusinessException.badRequest("字段正则过长或包含高风险回溯结构："+key);try{java.util.regex.Pattern.compile(pattern);}catch(Exception ex){throw BusinessException.badRequest("字段正则表达式不正确："+key);}}
    private java.math.BigDecimal decimal(Object value,String message){if(value==null||String.valueOf(value).isBlank())return null;try{return new java.math.BigDecimal(String.valueOf(value));}catch(Exception ex){throw BusinessException.badRequest(message);}}
    private Integer integer(Object value,String message){if(value==null||String.valueOf(value).isBlank())return null;try{return Integer.valueOf(String.valueOf(value));}catch(Exception ex){throw BusinessException.badRequest(message);}}

    private Map<String, Object> row(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("name", rs.getString("name"));
        value.put("type", rs.getString("type")); value.put("tag", rs.getString("tag"));
        value.put("description", rs.getString("description")); value.put("source", rs.getString("source"));
        value.put("content", json.map(rs.getString("content")));
        String schema = rs.getString("schema_definition"); value.put("schemaDefinition", schema == null ? null : json.map(schema));
        value.put("published", rs.getBoolean("published")); value.put("visibility", rs.getString("visibility"));
        value.put("enabled", rs.getBoolean("enabled")); value.put("disabledReason", rs.getString("disabled_reason"));
        value.put("disabledTime", rs.getObject("disabled_time")); value.put("creatorId", rs.getLong("creator_id"));
        value.put("creatorName", rs.getString("creator_name")); value.put("auditStatus", rs.getObject("audit_status"));
        value.put("auditComment", rs.getString("audit_comment")); value.put("version", rs.getInt("version"));
        value.put("dataScopeId", rs.getLong("data_scope_id")); value.put("createdTime", rs.getObject("created_time"));
        value.put("parentTemplateId", rs.getObject("parent_template_id")); value.put("sourceVersion", rs.getObject("source_version"));
        value.put("updatedTime", rs.getObject("updated_time")); value.put("favorited", rs.getBoolean("favorited"));
        value.put("lifecycleStatus", !rs.getBoolean("enabled") ? "DISABLED"
                : rs.getBoolean("published") ? "PUBLISHED"
                : rs.getObject("audit_status") != null && rs.getInt("audit_status") == 0 ? "PENDING_REVIEW"
                : rs.getObject("audit_status") != null && rs.getInt("audit_status") == 2 ? "REJECTED"
                : "PRIVATE".equals(rs.getString("visibility")) ? "PRIVATE_ACTIVE" : "DRAFT");
        return value;
    }

    private Map<String, Object> versionRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("version", rs.getInt("version")); value.put("name", rs.getString("name"));
        value.put("type", rs.getString("type")); value.put("tag", rs.getString("tag"));
        value.put("description", rs.getString("description")); value.put("source", rs.getString("source"));
        value.put("content", json.map(rs.getString("content")));
        String schema = rs.getString("schema_definition");
        value.put("schemaDefinition", schema == null ? null : json.map(schema));
        value.put("dataScopeId", rs.getLong("data_scope_id")); value.put("visibility", rs.getString("visibility"));
        value.put("restoredFromVersion", rs.getObject("restored_from_version"));
        value.put("changeReason", rs.getString("change_reason")); value.put("createdBy", rs.getLong("created_by"));
        value.put("createdByName", rs.getString("created_by_name")); value.put("createdTime", rs.getObject("created_time"));
        return value;
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) return "PRIVATE";
        String normalized = visibility.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("PRIVATE", "PUBLIC").contains(normalized)) throw BusinessException.badRequest("模板可见范围不正确");
        return normalized;
    }

    public record TemplateBody(String name, String type, String tag, String description, String source,
                               String visibility, Map<String, Object> content, Map<String, Object> schemaDefinition,
                               long dataScopeId) {}
    public record RestoreResult(long id, int version, long sourceTemplateId, int sourceVersion) {}
}
