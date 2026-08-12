package com.justeam.rdp.file;

import com.justeam.rdp.audit.AuditService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class FileStorageReconciler {
    private final JdbcClient jdbc;private final GridFsTemplate gridFs;private final AuditService audit;private final TransactionTemplate transactions;
    private long lastFileId;
    private ObjectId lastGridFsId;
    public FileStorageReconciler(JdbcClient jdbc,GridFsTemplate gridFs,AuditService audit,TransactionTemplate transactions){this.jdbc=jdbc;this.gridFs=gridFs;this.audit=audit;this.transactions=transactions;}

    @Scheduled(initialDelayString="${rdp.file.reconcile-initial-delay-ms:60000}",fixedDelayString="${rdp.file.reconcile-delay-ms:3600000}")
    public void reconcile(){int missing=0,orphans=0;
        List<FileRef> refs=fileBatch(lastFileId);if(refs.isEmpty()&&lastFileId>0){lastFileId=0;refs=fileBatch(0);}
        for(FileRef ref:refs){
            GridFSFile stored;try{stored=gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(ref.gridfsId()))));}
            catch(Exception ex){recordFailure("读取GridFS内容失败，本批未修改附件元数据",ex);return;}
            if(stored==null){int updated=jdbc.sql("UPDATE file_asset SET status='DELETED',deleted_time=now() WHERE id=:id AND status='AVAILABLE'").param("id",ref.id()).update();if(updated==1){missing++;audit.recordAs(null,"system","RECONCILE_MISSING","FILE","文件元数据引用的GridFS内容缺失",Map.of("fileId",ref.id()));}}
        }
        lastFileId=refs.size()==500?refs.get(refs.size()-1).id():0;
        List<GridFSFile> staleFiles;try{staleFiles=gridFsBatch(lastGridFsId);if(staleFiles.isEmpty()&&lastGridFsId!=null){lastGridFsId=null;staleFiles=gridFsBatch(null);}}
        catch(Exception ex){recordFailure("扫描GridFS孤儿文件失败，本批已停止",ex);return;}
        for(GridFSFile stored:staleFiles){try{if(deleteIfOrphan(stored))orphans++;}catch(Exception ex){recordFailure("清理GridFS孤儿文件失败，本批已停止",ex);return;}}
        lastGridFsId=staleFiles.size()==500?staleFiles.get(staleFiles.size()-1).getObjectId():null;
        if(missing>0||orphans>0)audit.recordAs(null,"system","RECONCILE","FILE","完成PG与GridFS一致性修复",Map.of("missingMarkedDeleted",missing,"orphanGridFsDeleted",orphans));
    }
    private List<FileRef> fileBatch(long afterId){return jdbc.sql("SELECT id,gridfs_id FROM file_asset WHERE status='AVAILABLE' AND id>:afterId ORDER BY id LIMIT 500").param("afterId",afterId).query(FileRef.class).list();}
    private List<GridFSFile> gridFsBatch(ObjectId afterId){Criteria criteria=Criteria.where("uploadDate").lt(Date.from(Instant.now().minusSeconds(3600)));if(afterId!=null)criteria=criteria.and("_id").gt(afterId);Query query=Query.query(criteria).with(Sort.by(Sort.Direction.ASC,"_id")).limit(500);List<GridFSFile> result=new ArrayList<>();gridFs.find(query).into(result);return result;}
    private boolean deleteIfOrphan(GridFSFile stored){Boolean deleted=transactions.execute(status->{Document metadata=stored.getMetadata();Number item=metadata==null?null:metadata.get("restoredFromArchiveItem",Number.class),version=metadata==null?null:metadata.get("restoredTransitionVersion",Number.class);if(item!=null&&version!=null){String active=jdbc.sql("SELECT state||':'||version FROM lifecycle_archive_item WHERE id=:id FOR UPDATE").param("id",item.longValue()).query(String.class).optional().orElse("");if(active.equals("RESTORING:"+version.intValue()))return false;}if(metadata!=null&&"UPLOAD_CHUNK".equals(metadata.getString("kind"))){String uploadId=metadata.getString("uploadId");if(uploadId!=null){long active=jdbc.sql("SELECT count(*) FROM file_upload_session WHERE id=CAST(:id AS uuid) AND ((status IN ('UPLOADING','FAILED') AND expires_time>now()) OR (status='COMPLETING' AND updated_time>now()-interval '6 hours'))").param("id",uploadId).query(Long.class).single();if(active>0)return false;}}String id=stored.getObjectId().toHexString();long references=jdbc.sql("SELECT count(*) FROM file_asset WHERE gridfs_id=:id AND status='AVAILABLE'").param("id",id).query(Long.class).single();if(references>0)return false;gridFs.delete(Query.query(Criteria.where("_id").is(stored.getObjectId())));return true;});return Boolean.TRUE.equals(deleted);}
    private void recordFailure(String description,Exception ex){String message=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();if(message.length()>500)message=message.substring(0,500);audit.recordAs(null,"system","RECONCILE_FAILED","FILE",description,Map.of("errorType",ex.getClass().getSimpleName(),"error",message));}
    private record FileRef(long id,String gridfsId){}
}
