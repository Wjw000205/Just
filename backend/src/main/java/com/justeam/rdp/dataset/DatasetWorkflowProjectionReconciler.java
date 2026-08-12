package com.justeam.rdp.dataset;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.JsonSupport;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatasetWorkflowProjectionReconciler {
    private static final int BATCH_SIZE=500;
    private static final int MAX_BACKFILL_BATCHES=20;
    private final JdbcClient jdbc;private final MongoTemplate mongo;private final AuditService audit;private final JsonSupport json;
    private long afterDatasetId;private String afterRecordId="";
    public DatasetWorkflowProjectionReconciler(JdbcClient jdbc,MongoTemplate mongo,AuditService audit,JsonSupport json){this.jdbc=jdbc;this.mongo=mongo;this.audit=audit;this.json=json;}

    /**
     * 升级回填专用快车道。搜索在 ready=false 时返回 503，因此不会用空投影产生假阴性或错误时间排序。
     */
    @Scheduled(initialDelayString="${rdp.workflow-projection-backfill-initial-delay-ms:1000}",fixedDelayString="${rdp.workflow-projection-backfill-delay-ms:5000}")
    public synchronized void backfill(){
        Summary summary=new Summary();
        long cursorDataset=0;String cursorRecord="";
        for(int i=0;i<MAX_BACKFILL_BATCHES;i++){
            List<Projection> batch=pendingBatch(cursorDataset,cursorRecord);if(batch.isEmpty())break;
            process(batch,summary);
            Projection last=batch.get(batch.size()-1);cursorDataset=last.datasetId();cursorRecord=last.recordId();
            if(batch.size()<BATCH_SIZE)break;
        }
        report("BACKFILL",summary);
    }

    /** 日常漂移校验低频扫描，校正 PG 搜索投影和 Mongo 工作流状态。 */
    @Scheduled(initialDelayString="${rdp.workflow-projection-initial-delay-ms:15000}",fixedDelayString="${rdp.workflow-projection-delay-ms:300000}")
    public synchronized void reconcile(){
        List<Projection> batch=driftBatch();
        if(batch.isEmpty()){afterDatasetId=0;afterRecordId="";return;}
        Summary summary=new Summary();process(batch,summary);
        Projection last=batch.get(batch.size()-1);afterDatasetId=last.datasetId();afterRecordId=last.recordId();
        report("RECONCILE",summary);
    }

    private void process(List<Projection> batch,Summary summary){
        for(Projection projection:batch){
            summary.scanned++;
            try{
                if(java.util.Set.of("CORRECTING","DELETING","ARCHIVING","ARCHIVED","RESTORING").contains(projection.status()))continue;
                String currentStatus=jdbc.sql("SELECT status FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record").param("dataset",projection.datasetId()).param("record",projection.recordId()).query(String.class).optional().orElse("DELETED");if(!projection.status().equals(currentStatus)||java.util.Set.of("CORRECTING","DELETING","ARCHIVING","ARCHIVED","RESTORING").contains(currentStatus))continue;
                String collection="dataset_data_"+projection.datasetId();
                var mongoResult=mongo.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(projection.recordId())).and("workflowStatus").ne(projection.status())),Update.update("workflowStatus",projection.status()),collection);
                summary.repaired+=mongoResult.getModifiedCount();
                Document document=mongo.findOne(Query.query(Criteria.where("_id").is(new ObjectId(projection.recordId())).and("deleted").ne(true)),Document.class,collection);
                if(document==null){
                    summary.orphans++;
                    summary.repaired+=jdbc.sql("UPDATE dataset_record_workflow SET status='DELETED',search_projection_ready=TRUE,updated_time=now() WHERE dataset_id=:dataset AND record_id=:record AND status=:expected AND status NOT IN ('CORRECTING','DELETING','ARCHIVING','ARCHIVED','RESTORING')")
                            .param("dataset",projection.datasetId()).param("record",projection.recordId()).param("expected",projection.status()).update();
                    continue;
                }
                int updated=jdbc.sql("""
                    UPDATE dataset_record_workflow SET search_data=CAST(:data AS jsonb),
                        record_created_time=:created,record_updated_time=:updated,search_projection_ready=TRUE
                    WHERE dataset_id=:dataset AND record_id=:record
                      AND (search_projection_ready=FALSE OR search_data IS DISTINCT FROM CAST(:data AS jsonb)
                           OR record_created_time IS DISTINCT FROM :created OR record_updated_time IS DISTINCT FROM :updated)
                    """).param("data",json.write(document.get("data")))
                        .param("created",timestamp(document.get("createdTime"),projection.recordCreatedTime()))
                        .param("updated",timestamp(document.get("updatedTime")!=null?document.get("updatedTime"):document.get("createdTime"),projection.recordUpdatedTime()))
                        .param("dataset",projection.datasetId()).param("record",projection.recordId()).update();
                summary.repaired+=updated;
            }catch(Exception ex){
                summary.failed++;
                if(summary.firstError==null){String message=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();summary.firstError=message.substring(0,Math.min(message.length(),500));}
            }
        }
    }

    private List<Projection> pendingBatch(long cursorDataset,String cursorRecord){return jdbc.sql("""
            SELECT dataset_id,record_id,status,record_created_time,record_updated_time
            FROM dataset_record_workflow WHERE search_projection_ready=FALSE AND status NOT IN ('DELETED','CORRECTING','DELETING','ARCHIVING','ARCHIVED','RESTORING')
              AND (dataset_id>:dataset OR (dataset_id=:dataset AND record_id>:record))
            ORDER BY dataset_id,record_id LIMIT :limit
            """).param("dataset",cursorDataset).param("record",cursorRecord).param("limit",BATCH_SIZE).query(Projection.class).list();}

    private List<Projection> driftBatch(){return jdbc.sql("""
            SELECT dataset_id,record_id,status,record_created_time,record_updated_time
            FROM dataset_record_workflow
            WHERE status NOT IN ('CORRECTING','DELETING','ARCHIVING','ARCHIVED','RESTORING')
              AND (dataset_id>:dataset OR (dataset_id=:dataset AND record_id>:record))
            ORDER BY dataset_id,record_id LIMIT :limit
            """).param("dataset",afterDatasetId).param("record",afterRecordId).param("limit",BATCH_SIZE).query(Projection.class).list();}

    private Timestamp timestamp(Object value,Instant fallback){if(value instanceof Instant instant)return Timestamp.from(instant);if(value instanceof java.util.Date date)return new Timestamp(date.getTime());return Timestamp.from(fallback==null?Instant.now():fallback);}
    private void report(String operation,Summary summary){if(summary.scanned==0)return;Map<String,Object> details=new LinkedHashMap<>();details.put("scanned",summary.scanned);details.put("repaired",summary.repaired);details.put("orphansMarkedDeleted",summary.orphans);details.put("failed",summary.failed);if(summary.firstError!=null)details.put("firstError",summary.firstError);audit.recordAs(null,"system",summary.failed>0?operation+"_FAILED":operation,"DATASET",summary.failed>0?"数据记录检索投影修复存在失败":"数据记录检索投影修复完成",details);}

    private record Projection(long datasetId,String recordId,String status,Instant recordCreatedTime,Instant recordUpdatedTime){}
    private static final class Summary{int scanned;long repaired;int orphans;int failed;String firstError;}
}
