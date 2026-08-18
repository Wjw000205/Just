package com.justeam.rdp.dataset;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;

@Service
public class CrossStoreOutboxService {
    private static final DateTimeFormatter BUSINESS_DATE=DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Shanghai"));
    private final JdbcClient jdbc;
    private final JsonSupport json;
    private final AuditService audit;

    public CrossStoreOutboxService(JdbcClient jdbc, JsonSupport json, AuditService audit) {
        this.jdbc = jdbc;
        this.json = json;
        this.audit = audit;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event prepare(UUID key, long datasetId, String recordId, String operation,
                         Map<String, Object> payload, Map<String, Object> requestIdentity) {
        String fingerprint = fingerprint(Map.of(
                "operation", operation,
                "datasetId", datasetId,
                "recordId", recordId == null ? "" : recordId,
                "request", requestIdentity));
        UserPrincipal actor = CurrentUser.require();
        jdbc.sql("""
                INSERT INTO cross_store_outbox(event_key,aggregate_type,aggregate_id,aggregate_record_id,
                                               operation,request_fingerprint,payload,actor_id,actor_username)
                VALUES (:key,'DATASET_RECORD',:datasetId,:recordId,:operation,:fingerprint,CAST(:payload AS jsonb),
                        :actorId,:actorUsername)
                ON CONFLICT (event_key) DO NOTHING
                """).param("key", key).param("datasetId", datasetId).param("operation", operation)
                .param("recordId", recordId).param("fingerprint", fingerprint)
                .param("actorId", actor.id()).param("actorUsername", actor.username())
                .param("payload", json.write(payload)).update();
        Event event = jdbc.sql("""
                SELECT id,event_key,aggregate_id,aggregate_record_id,operation,request_fingerprint,actor_id,actor_username,
                       created_time,status,payload::text AS payload,attempts,FALSE AS leased,NULL::uuid AS processing_token
                FROM cross_store_outbox WHERE event_key=:key
                """).param("key", key).query(Event.class).single();
        if (event.aggregateId() != datasetId || !event.operation().equals(operation)
                || !Objects.equals(event.aggregateRecordId(), recordId)
                || !fingerprint.equals(event.requestFingerprint())) {
            throw new BusinessException(409, "幂等键已用于其他请求，不能复用");
        }
        if("MANUAL_REVIEW".equals(event.status()))jdbc.sql("UPDATE cross_store_outbox SET status='FAILED',attempts=0,last_error='SAME_REQUEST_REACTIVATED' WHERE id=:id AND status='MANUAL_REVIEW'").param("id",event.id()).update();
        return event;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event prepareDelete(UUID key,long datasetId,String recordId,Map<String,Object> payload,Map<String,Object> requestIdentity,int expectedVersion){Event event=prepare(key,datasetId,recordId,"DELETE",payload,requestIdentity);if("DONE".equals(event.status()))return event;DeleteReservation current=jdbc.sql("SELECT status,record_version,delete_event_id FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record FOR UPDATE").param("dataset",datasetId).param("record",recordId).query((rs,n)->new DeleteReservation(rs.getString("status"),rs.getInt("record_version"),rs.getObject("delete_event_id")==null?null:rs.getLong("delete_event_id"))).optional().orElseThrow(()->BusinessException.notFound("记录工作流不存在"));if("DELETING".equals(current.status())){if(Objects.equals(current.eventId(),event.id()))return event;throw new BusinessException(409,"记录已有删除作业");}if(!java.util.Set.of("DRAFT","REJECTED").contains(current.status()))throw BusinessException.badRequest("仅草稿或已驳回记录可删除");if(current.version()!=expectedVersion)throw new BusinessException(409,"记录工作流版本已变化");long holds=jdbc.sql("SELECT count(*) FROM lifecycle_hold WHERE resource_type='DATASET_RECORD' AND resource_ref=:ref AND active=TRUE AND (valid_to IS NULL OR valid_to>now())").param("ref",datasetId+":"+recordId).query(Long.class).single();if(holds>0)throw BusinessException.badRequest("记录处于质量或法律保留，禁止删除");long files=jdbc.sql("SELECT count(*) FROM file_asset WHERE business_type='DATASET_RECORD' AND business_ref=:ref AND status='AVAILABLE'").param("ref",datasetId+":"+recordId).query(Long.class).single();if(files>0)throw new BusinessException(409,"记录仍有关联附件，请先受控删除附件");jdbc.sql("UPDATE dataset_record_workflow SET status='DELETING',delete_event_id=:event,delete_previous_status=:previous,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record").param("event",event.id()).param("previous",current.status()).param("dataset",datasetId).param("record",recordId).update();return event;}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event prepareCorrection(UUID key,long datasetId,String recordId,Map<String,Object> payload,
                                   Map<String,Object> requestIdentity,int expectedVersion){
        Event event=prepare(key,datasetId,recordId,"UPDATE",payload,requestIdentity);
        if("DONE".equals(event.status()))return event;
        CorrectionReservation current=jdbc.sql("SELECT status,record_version,correction_event_id FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record FOR UPDATE")
                .param("dataset",datasetId).param("record",recordId)
                .query((rs,n)->new CorrectionReservation(rs.getString("status"),rs.getInt("record_version"),rs.getObject("correction_event_id")==null?null:rs.getLong("correction_event_id")))
                .optional().orElseThrow(()->BusinessException.notFound("记录工作流不存在"));
        if("CORRECTING".equals(current.status())){
            if(Objects.equals(current.eventId(),event.id()))return event;
            throw new BusinessException(409,"记录已有受控更正作业");
        }
        if(!java.util.Set.of("DRAFT","REJECTED").contains(current.status()))
            throw BusinessException.badRequest("仅草稿或已驳回记录可更正；已审核/发布记录需创建新版本");
        if(current.version()!=expectedVersion)throw new BusinessException(409,"记录工作流版本已变化");
        int changed=jdbc.sql("UPDATE dataset_record_workflow SET status='CORRECTING',correction_event_id=:event,correction_previous_status=:previous,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record AND status=:previous AND record_version=:version")
                .param("event",event.id()).param("previous",current.status()).param("dataset",datasetId).param("record",recordId).param("version",expectedVersion).update();
        if(changed!=1)throw new BusinessException(409,"记录状态已变化，请刷新后重试");
        return event;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Event> recoverableEvents() {
        // A worker may crash after taking the tenth and final lease. Publish that
        // terminal state before selecting new work so PROCESSING can never be
        // stranded forever behind the attempts < 10 predicate.
        jdbc.sql("""
                UPDATE cross_store_outbox
                   SET status='MANUAL_REVIEW',last_error=coalesce(last_error,'FINAL_LEASE_EXPIRED'),
                       processing_token=NULL,processing_started_time=NULL
                 WHERE status='PROCESSING' AND attempts>=10
                   AND processing_started_time < now()-interval '5 minutes'
                """).update();
        UUID token=UUID.randomUUID();
        return jdbc.sql("""
                WITH candidates AS (
                    SELECT id FROM cross_store_outbox
                    WHERE ((status IN ('PENDING','FAILED') AND created_time < now()-interval '30 seconds')
                           OR (status='PROCESSING' AND processing_started_time < now()-interval '5 minutes'))
                      AND attempts < 10
                    ORDER BY created_time FOR UPDATE SKIP LOCKED LIMIT 50
                )
                UPDATE cross_store_outbox o SET status='PROCESSING',attempts=o.attempts+1,last_error=NULL,
                       processing_token=:token,processing_started_time=now()
                FROM candidates c WHERE o.id=c.id
                RETURNING o.id,o.event_key,o.aggregate_id,o.aggregate_record_id,o.operation,o.request_fingerprint,
                          o.actor_id,o.actor_username,o.created_time,o.status,o.payload::text AS payload,o.attempts,TRUE AS leased,
                          o.processing_token
                """).param("token",token).query(Event.class).list();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeCreate(Event event, long datasetId,String recordId, Map<String, Object> details) {
        UUID token=claim(event);if(token==null)return;
        String businessCode=businessCode(event);
        jdbc.sql("UPDATE data_dataset SET data_count=data_count+1, updated_time=now() WHERE id=:id")
                .param("id", datasetId).update();
        jdbc.sql("""
                INSERT INTO dataset_record_workflow(dataset_id,record_id,business_code,record_version,owner_id,status,search_data,
                                                    record_created_time,record_updated_time,search_projection_ready)
                VALUES (:dataset,:record,:businessCode,1,:owner,'DRAFT',CAST(:data AS jsonb),now(),now(),TRUE)
                ON CONFLICT(dataset_id,record_id) DO UPDATE SET search_data=EXCLUDED.search_data,
                    business_code=coalesce(dataset_record_workflow.business_code,EXCLUDED.business_code),
                    record_updated_time=EXCLUDED.record_updated_time,search_projection_ready=TRUE
                """).param("dataset",datasetId).param("record",recordId).param("businessCode",businessCode).param("owner",event.actorId())
                .param("data",json.write(details.getOrDefault("after",Map.of()))).update();
        done(event.id(),token);
        Map<String,Object> completedDetails=new LinkedHashMap<>(details);completedDetails.put("businessCode",businessCode);
        audit.recordAs(event.actorId(), event.actorUsername(), "CREATE_RECORD", "DATASET", "新增数据集记录", completedDetails);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeUpdate(Event event, Map<String, Object> details) {
        UUID token=claim(event);if(token==null)return;
        int nextVersion=((Number)details.getOrDefault("expectedVersion",0)).intValue()+1;
        int workflow=jdbc.sql("""
                UPDATE dataset_record_workflow SET record_version=:version,
                    status='DRAFT',correction_event_id=NULL,correction_previous_status=NULL,
                    submitted_by=NULL,submitted_time=NULL,reviewed_by=NULL,reviewed_time=NULL,review_comment=NULL,
                    published_by=NULL,published_time=NULL,
                    search_data=CAST(:data AS jsonb),record_updated_time=now(),search_projection_ready=TRUE,updated_time=now()
                WHERE dataset_id=:dataset AND record_id=:record AND status='CORRECTING'
                  AND correction_event_id=:event AND record_version=:expected
                """).param("version",nextVersion).param("data",json.write(details.getOrDefault("after",Map.of())))
                .param("dataset",event.aggregateId()).param("record",event.aggregateRecordId()).param("event",event.id())
                .param("expected",nextVersion-1).update();
        if(workflow!=1)throw new IllegalStateException("受控更正预占状态已变化");
        done(event.id(),token);
        audit.recordAs(event.actorId(), event.actorUsername(), "CONTROLLED_CORRECTION", "DATASET", "受控更正数据集记录", details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeDelete(Event event, long datasetId, Map<String, Object> details) {
        UUID token=claim(event);if(token==null)return;
        int workflow=jdbc.sql("UPDATE dataset_record_workflow SET status='DELETED',delete_event_id=NULL,delete_previous_status=NULL,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record AND status='DELETING' AND delete_event_id=:event")
                .param("dataset",datasetId).param("record",event.aggregateRecordId()).param("event",event.id()).update();
        if(workflow!=1)throw new IllegalStateException("删除预占状态已变化");
        jdbc.sql("UPDATE data_dataset SET data_count=GREATEST(data_count-1,0),updated_time=now() WHERE id=:id")
                .param("id", datasetId).update();
        done(event.id(),token);
        audit.recordAs(event.actorId(), event.actorUsername(), "CONTROLLED_DELETE", "DATASET", "受控删除数据集记录", details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(long eventId, Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        if (message.length() > 2000) message = message.substring(0, 2000);
        jdbc.sql("""
                UPDATE cross_store_outbox SET status=CASE WHEN attempts+1>=10 THEN 'MANUAL_REVIEW' ELSE 'FAILED' END,
                       attempts=attempts+1,last_error=:error,processing_token=NULL,processing_started_time=NULL
                WHERE id=:id AND status <> 'DONE'
                """).param("error", message).param("id", eventId).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reject(long eventId, Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        jdbc.sql("UPDATE cross_store_outbox SET status='REJECTED',attempts=attempts+1,last_error=:error WHERE id=:id AND status<>'DONE'")
                .param("error", message).param("id", eventId).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectDelete(Event event,Exception ex){jdbc.sql("UPDATE dataset_record_workflow SET status=delete_previous_status,delete_event_id=NULL,delete_previous_status=NULL,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record AND status='DELETING' AND delete_event_id=:event").param("dataset",event.aggregateId()).param("record",event.aggregateRecordId()).param("event",event.id()).update();reject(event.id(),ex);}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectCorrection(Event event,Exception ex){
        jdbc.sql("UPDATE dataset_record_workflow SET status=correction_previous_status,correction_event_id=NULL,correction_previous_status=NULL,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record AND status='CORRECTING' AND correction_event_id=:event")
                .param("dataset",event.aggregateId()).param("record",event.aggregateRecordId()).param("event",event.id()).update();
        reject(event.id(),ex);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryMiss(Event event, String message) {
        jdbc.sql("""
                UPDATE cross_store_outbox SET status=CASE WHEN attempts>=10 THEN 'MANUAL_REVIEW' ELSE 'FAILED' END,
                       last_error=:error,processing_token=NULL,processing_started_time=NULL
                WHERE id=:id AND status='PROCESSING' AND processing_token=:token
                """).param("error", message).param("id",event.id()).param("token",event.processingToken()).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failLeased(Event event,Exception ex){String message=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();if(message.length()>2000)message=message.substring(0,2000);retryMiss(event,message);}

    public Map<String, Object> details(Event event) {
        return json.map(event.payload());
    }

    public String businessCode(Event event) {
        if(!"CREATE".equals(event.operation()))throw new IllegalArgumentException("仅创建事件具有业务标识码");
        String prefix=jdbc.sql("SELECT record_code_prefix FROM data_dataset WHERE id=:id AND deleted=0")
                .param("id",event.aggregateId()).query(String.class).optional()
                .orElseThrow(()->BusinessException.notFound("数据集不存在"));
        return prefix+"-"+BUSINESS_DATE.format(event.createdTime())+"-"+String.format(java.util.Locale.ROOT,"%06d",event.id());
    }

    private UUID claim(Event event) {
        if(event.leased()){
            boolean valid=jdbc.sql("SELECT count(*) FROM cross_store_outbox WHERE id=:id AND status='PROCESSING' AND processing_token=:token")
                    .param("id",event.id()).param("token",event.processingToken()).query(Long.class).single()==1;
            if(!valid)throw new BusinessException(409,"补偿任务租约已失效");return event.processingToken();
        }
        UUID token=UUID.randomUUID();
        int updated = jdbc.sql("""
                UPDATE cross_store_outbox SET status='PROCESSING',attempts=attempts+1,last_error=NULL,
                       processing_token=:token,processing_started_time=now()
                WHERE id=:id AND status IN ('PENDING','FAILED')
                """).param("id",event.id()).param("token",token).update();
        if (updated == 1) return token;
        String status = jdbc.sql("SELECT status FROM cross_store_outbox WHERE id=:id")
                .param("id",event.id()).query(String.class).single();
        // A concurrent duplicate carrying the same verified idempotency key may
        // observe the original request while it is completing. It must not
        // reject or roll back that request; the lease owner/reconciler remains
        // responsible for the single PG completion.
        if ("DONE".equals(status) || "PROCESSING".equals(status)) return null;
        throw new BusinessException(409, "该幂等事件正在处理中，请稍后查询结果");
    }

    private void done(long eventId,UUID token) {
        int updated = jdbc.sql("""
                UPDATE cross_store_outbox SET status='DONE',processed_time=now(),processing_token=NULL,processing_started_time=NULL
                WHERE id=:id AND status='PROCESSING' AND processing_token=:token
                """).param("id", eventId).param("token",token).update();
        if (updated != 1) throw new IllegalStateException("Outbox状态迁移失败");
    }

    private String fingerprint(Object request) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.canonical(request).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("请求指纹计算失败", ex);
        }
    }

    public record Event(long id, UUID eventKey, long aggregateId, String aggregateRecordId,
                        String operation, String requestFingerprint, Long actorId, String actorUsername,
                        Instant createdTime,String status, String payload,int attempts,boolean leased,UUID processingToken) {}
    private record DeleteReservation(String status,int version,Long eventId){}
    private record CorrectionReservation(String status,int version,Long eventId){}
}
