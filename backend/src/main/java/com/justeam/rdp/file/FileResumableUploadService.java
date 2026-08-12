package com.justeam.rdp.file;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileResumableUploadService {
    private static final String SHA_PATTERN="^[0-9a-fA-F]{64}$";
    private final JdbcClient jdbc;
    private final GridFsTemplate gridFs;
    private final FileAssetService files;
    private final AuditService audit;
    private final TransactionTemplate transactions;
    private final int chunkSize;
    private final long maxSize;
    private final long ttlSeconds;
    private final int maxActiveSessions;
    private final long maxPendingBytes;

    public FileResumableUploadService(JdbcClient jdbc,GridFsTemplate gridFs,FileAssetService files,AuditService audit,
                                      TransactionTemplate transactions,
                                      @Value("${rdp.file.chunk-size-bytes:4194304}") int chunkSize,
                                      @Value("${rdp.file.max-size-bytes:21474836480}") long maxSize,
                                      @Value("${rdp.file.upload-session-seconds:86400}") long ttlSeconds,
                                      @Value("${rdp.file.max-active-sessions-per-user:8}")int maxActiveSessions,
                                      @Value("${rdp.file.max-pending-bytes-per-user:42949672960}")long maxPendingBytes){
        this.jdbc=jdbc;this.gridFs=gridFs;this.files=files;this.audit=audit;this.transactions=transactions;
        this.chunkSize=Math.max(262144,Math.min(chunkSize,16*1024*1024));this.maxSize=maxSize;this.ttlSeconds=ttlSeconds;this.maxActiveSessions=Math.max(1,maxActiveSessions);this.maxPendingBytes=Math.max(maxSize,maxPendingBytes);
    }

    public Map<String,Object> initiate(Initiate body){
        if(body==null||body.uploadId()==null)throw BusinessException.badRequest("uploadId不能为空");
        if(body.sizeBytes()<=0||body.sizeBytes()>maxSize)throw BusinessException.badRequest("文件大小超出允许范围");
        if(body.sha256()==null||!body.sha256().matches(SHA_PATTERN))throw BusinessException.badRequest("必须提供64位整文件SHA-256");
        String filename=safeFilename(body.originalName());
        String contentType=body.contentType()==null||body.contentType().isBlank()?"application/octet-stream":body.contentType();
        try{contentType=org.springframework.http.MediaType.parseMediaType(contentType).toString();}catch(Exception ex){throw BusinessException.badRequest("文件内容类型不正确");}
        FileAssetService.UploadTarget target=files.validateUploadTarget(body.businessType(),body.businessRef(),body.dataScopeId());
        UserPrincipal user=CurrentUser.require();int total=(int)((body.sizeBytes()+chunkSize-1)/chunkSize);if(total>10000)throw BusinessException.badRequest("文件分片数不能超过10000");
        String finalContentType=contentType;
        Session session;try{session=transactions.execute(status->{
            jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) locked").param("key",991728000000L+user.id()).query(Long.class).single();
            Session existing=jdbc.sql("SELECT * FROM file_upload_session WHERE id=:id FOR UPDATE").param("id",body.uploadId()).query(this::sessionRow).optional().orElse(null);
            if(existing!=null){requireOwner(existing,user);if(Set.of("EXPIRED","CANCELLED").contains(existing.status())||existing.expiresTime().isBefore(Instant.now()))throw new BusinessException(410,"上传会话已失效，请使用新的uploadId重新初始化");if(existing.sizeBytes()!=body.sizeBytes()||!existing.sha256().equalsIgnoreCase(body.sha256())||!existing.businessType().equals(target.businessType())||!existing.businessRef().equals(target.businessRef())||existing.dataScopeId()!=target.dataScopeId())throw new BusinessException(409,"uploadId已用于其他文件");return existing;}
            Map<String,Object> quota=jdbc.sql("""
                    SELECT count(*) FILTER (WHERE (status IN ('UPLOADING','FAILED') AND expires_time>now()) OR (status='COMPLETING' AND updated_time>now()-interval '6 hours')) AS active,
                           coalesce(sum(size_bytes) FILTER (WHERE (status IN ('UPLOADING','FAILED') AND expires_time>now()) OR (status='COMPLETING' AND updated_time>now()-interval '6 hours')),0) AS declared,
                           coalesce((SELECT sum(c.size_bytes) FROM file_upload_chunk c JOIN file_upload_session s2 ON s2.id=c.upload_id WHERE s2.uploaded_by=:actor AND s2.status<>'COMPLETED'),0) AS stored
                    FROM file_upload_session WHERE uploaded_by=:actor
                    """).param("actor",user.id()).query((rs,n)->Map.<String,Object>of("active",rs.getLong("active"),"declared",rs.getLong("declared"),"stored",rs.getLong("stored"))).single();
            if(((Number)quota.get("active")).longValue()>=maxActiveSessions)throw new BusinessException(429,"未完成上传会话数量已达上限，请完成或取消后重试");long declared=((Number)quota.get("declared")).longValue(),stored=((Number)quota.get("stored")).longValue(),available=maxPendingBytes-body.sizeBytes();if(declared>available||stored>available-declared)throw new BusinessException(429,"未完成上传容量已达上限，请完成或取消后重试");
            jdbc.sql("""
                    INSERT INTO file_upload_session(id,original_name,content_type,size_bytes,expected_sha256,business_type,business_ref,
                      data_scope_id,chunk_size,total_chunks,uploaded_by,expires_time)
                    VALUES (:id,:name,:contentType,:size,:sha,:type,:ref,:scope,:chunkSize,:total,:actor,now()+(:ttl||' seconds')::interval)
                    """).param("id",body.uploadId()).param("name",filename).param("contentType",finalContentType).param("size",body.sizeBytes())
                    .param("sha",body.sha256().toLowerCase(java.util.Locale.ROOT)).param("type",target.businessType()).param("ref",target.businessRef())
                    .param("scope",target.dataScopeId()).param("chunkSize",chunkSize).param("total",total).param("actor",user.id()).param("ttl",ttlSeconds).update();
            return jdbc.sql("SELECT * FROM file_upload_session WHERE id=:id").param("id",body.uploadId()).query(this::sessionRow).single();
        });}catch(BusinessException ex){if(ex.code()==429)audit.recordIndependent(user.id(),user.username(),"UPLOAD_QUOTA_REJECTED","FILE","分片上传用户配额拒绝",Map.of("maxActiveSessions",maxActiveSessions,"maxPendingBytes",maxPendingBytes,"requestedBytes",body.sizeBytes()));throw ex;}
        audit.record("UPLOAD_INIT","FILE","初始化分片上传",Map.of("uploadId",session.id(),"name",session.name(),"sizeBytes",session.sizeBytes(),"sha256",session.sha256(),"businessType",session.businessType(),"businessRef",session.businessRef()));
        return view(session);
    }

    public Map<String,Object> status(UUID id){Session session=requireSession(id,false);files.validateUploadTarget(session.businessType(),session.businessRef(),session.dataScopeId());return view(session);}

    public Map<String,Object> uploadChunk(UUID id,int index,String sha256,String contentRange,InputStream raw){
        if(index<0||sha256==null||!sha256.matches(SHA_PATTERN))throw BusinessException.badRequest("分片序号或SHA-256不正确");
        Session session=requireSession(id,true);files.validateUploadTarget(session.businessType(),session.businessRef(),session.dataScopeId());
        if(index>=session.totalChunks())throw BusinessException.badRequest("分片序号超出范围");
        int expected=(int)(index==session.totalChunks()-1?session.sizeBytes()-(long)index*session.chunkSize():session.chunkSize());
        String expectedRange="bytes "+((long)index*session.chunkSize())+"-"+((long)index*session.chunkSize()+expected-1)+"/"+session.sizeBytes();
        if(contentRange==null||!contentRange.equals(expectedRange))throw BusinessException.badRequest("Content-Range与上传会话不一致");
        ObjectId gridId=null;boolean registered=false;
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");CountingInputStream counted=new CountingInputStream(raw,expected);
            Document metadata=new Document("kind","UPLOAD_CHUNK").append("uploadId",id.toString()).append("chunkIndex",index);
            gridId=gridFs.store(new DigestInputStream(counted,digest),id+"-"+index,"application/octet-stream",metadata);
            String actual=HexFormat.of().formatHex(digest.digest());if(counted.count()!=expected)throw BusinessException.badRequest("分片大小与Content-Range不一致");if(!actual.equalsIgnoreCase(sha256))throw BusinessException.badRequest("分片SHA-256校验失败");
            ObjectId storedId=gridId;Boolean inserted=transactions.execute(status->{Session locked=locked(id);requireActive(locked);Map<String,Object> existing=jdbc.sql("SELECT size_bytes,sha256,gridfs_id FROM file_upload_chunk WHERE upload_id=:id AND chunk_index=:idx").param("id",id).param("idx",index).query((rs,n)->{Map<String,Object> value=new LinkedHashMap<>();value.put("size",rs.getInt("size_bytes"));value.put("sha",rs.getString("sha256"));value.put("grid",rs.getString("gridfs_id"));return value;}).optional().orElse(null);if(existing!=null){if(((Number)existing.get("size")).intValue()==expected&&String.valueOf(existing.get("sha")).equalsIgnoreCase(actual)){GridFSFile previous;try{previous=gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(String.valueOf(existing.get("grid"))))));}catch(Exception ex){throw new BusinessException(503,"已有分片状态暂不可确认");}if(previous!=null)return false;jdbc.sql("UPDATE file_upload_chunk SET gridfs_id=:grid,created_time=now() WHERE upload_id=:id AND chunk_index=:idx").param("grid",storedId.toHexString()).param("id",id).param("idx",index).update();return true;}throw new BusinessException(409,"该序号已存在不同分片，请先取消或重新初始化上传");}jdbc.sql("INSERT INTO file_upload_chunk(upload_id,chunk_index,size_bytes,sha256,gridfs_id) VALUES (:id,:idx,:size,:sha,:grid)").param("id",id).param("idx",index).param("size",expected).param("sha",actual).param("grid",storedId.toHexString()).update();jdbc.sql("UPDATE file_upload_session SET updated_time=now(),expires_time=now()+(:ttl||' seconds')::interval WHERE id=:id").param("ttl",ttlSeconds).param("id",id).update();return true;});
            registered=Boolean.TRUE.equals(inserted);if(!registered)gridFs.delete(Query.query(Criteria.where("_id").is(gridId)));
            return view(jdbc.sql("SELECT * FROM file_upload_session WHERE id=:id").param("id",id).query(this::sessionRow).single());
        }catch(BusinessException ex){if(gridId!=null&&!registered)gridFs.delete(Query.query(Criteria.where("_id").is(gridId)));throw ex;}catch(Exception ex){if(gridId!=null&&!registered)gridFs.delete(Query.query(Criteria.where("_id").is(gridId)));throw new BusinessException(500,"分片保存失败");}
    }

    public Map<String,Object> complete(UUID id){
        UserPrincipal actor=CurrentUser.require();Session initial=requireSession(id,false);
        if("COMPLETED".equals(initial.status()))return view(initial);
        files.validateUploadTarget(initial.businessType(),initial.businessRef(),initial.dataScopeId());
        Session session=transactions.execute(status->{Session locked=locked(id);requireOwner(locked,actor);requireCompletable(locked);long chunks=jdbc.sql("SELECT count(*) FROM file_upload_chunk WHERE upload_id=:id").param("id",id).query(Long.class).single();long bytes=jdbc.sql("SELECT coalesce(sum(size_bytes),0) FROM file_upload_chunk WHERE upload_id=:id").param("id",id).query(Long.class).single();if(chunks!=locked.totalChunks()||bytes!=locked.sizeBytes())throw new BusinessException(409,"分片尚未全部上传");jdbc.sql("UPDATE file_upload_session SET status='COMPLETING',version=version+1,updated_time=now(),expires_time=now()+(:ttl||' seconds')::interval WHERE id=:id").param("ttl",ttlSeconds).param("id",id).update();return locked;});
        List<Chunk> chunks=jdbc.sql("SELECT chunk_index,size_bytes,sha256,gridfs_id FROM file_upload_chunk WHERE upload_id=:id ORDER BY chunk_index").param("id",id).query(Chunk.class).list();
        boolean committed=false;
        try{
            try(InputStream combined=new ChunkSequenceInputStream(chunks)){
                Long fileId=transactions.execute(status->{long created=files.storeStream(combined,session.sizeBytes(),session.sha256(),session.name(),session.contentType(),session.businessType(),session.businessRef(),session.dataScopeId());int changed=jdbc.sql("UPDATE file_upload_session SET status='COMPLETED',file_asset_id=:file,updated_time=now() WHERE id=:id AND status='COMPLETING'").param("file",created).param("id",id).update();if(changed!=1)throw new BusinessException(409,"上传会话状态已变化");audit.record("UPLOAD_COMPLETE","FILE","分片上传合并完成",Map.of("uploadId",id,"fileId",created,"sizeBytes",session.sizeBytes(),"sha256",session.sha256(),"businessType",session.businessType(),"businessRef",session.businessRef()));return created;});
                committed=true;try{cleanupChunks(id);}catch(Exception ignored){}return completedView(session,fileId);
            }
        }catch(BusinessException ex){if(!committed){resetAfterFailure(id);audit.record("UPLOAD_FAILED","FILE","分片上传合并失败",Map.of("uploadId",id,"error",safeError(ex)));}throw ex;}catch(Exception ex){if(!committed){resetAfterFailure(id);audit.record("UPLOAD_FAILED","FILE","分片上传合并失败",Map.of("uploadId",id,"error",safeError(ex)));throw new BusinessException(500,"文件合并失败");}return Map.of("uploadId",id,"status","COMPLETED","fileId",jdbc.sql("SELECT file_asset_id FROM file_upload_session WHERE id=:id").param("id",id).query(Long.class).single());}
    }

    public void cancel(UUID id){Session session=transactions.execute(status->{Session locked=locked(id);requireOwner(locked,CurrentUser.require());if("COMPLETED".equals(locked.status()))throw new BusinessException(409,"已完成上传不能取消");jdbc.sql("UPDATE file_upload_session SET status='CANCELLED',updated_time=now() WHERE id=:id").param("id",id).update();return locked;});cleanupChunks(id);audit.record("UPLOAD_CANCEL","FILE","取消分片上传",Map.of("uploadId",id,"name",session.name(),"sizeBytes",session.sizeBytes()));}

    @Scheduled(initialDelayString="${rdp.file.upload-cleanup-initial-delay-ms:120000}",fixedDelayString="${rdp.file.upload-cleanup-delay-ms:3600000}")
    public void expire(){List<UUID> stale=jdbc.sql("UPDATE file_upload_session SET status='FAILED',updated_time=now() WHERE status='COMPLETING' AND updated_time<now()-interval '6 hours' RETURNING id").query(UUID.class).list();for(UUID id:stale)audit.recordAs(null,"system","UPLOAD_FAILED","FILE","中断的分片合并已恢复为可重试状态",Map.of("uploadId",id));List<UUID> expired=jdbc.sql("UPDATE file_upload_session SET status='EXPIRED',updated_time=now() WHERE status IN ('UPLOADING','FAILED') AND expires_time<now() RETURNING id").query(UUID.class).list();for(UUID id:expired){cleanupChunks(id);audit.recordAs(null,"system","UPLOAD_EXPIRED","FILE","过期分片上传已清理",Map.of("uploadId",id));}}

    private Session requireSession(UUID id,boolean active){Session session=jdbc.sql("SELECT * FROM file_upload_session WHERE id=:id").param("id",id).query(this::sessionRow).optional().orElseThrow(()->BusinessException.notFound("上传会话不存在"));requireOwner(session,CurrentUser.require());if(session.expiresTime().isBefore(Instant.now())&&!"COMPLETED".equals(session.status()))throw new BusinessException(410,"上传会话已过期");if(active)requireActive(session);return session;}
    private Session locked(UUID id){return jdbc.sql("SELECT * FROM file_upload_session WHERE id=:id FOR UPDATE").param("id",id).query(this::sessionRow).optional().orElseThrow(()->BusinessException.notFound("上传会话不存在"));}
    private void requireOwner(Session session,UserPrincipal user){if(!user.admin()&&session.uploadedBy()!=user.id())throw BusinessException.notFound("上传会话不存在");}
    private void requireActive(Session session){if(!"UPLOADING".equals(session.status()))throw new BusinessException(409,"上传会话当前不可接收分片");}
    private void requireCompletable(Session session){if(!Set.of("UPLOADING","FAILED").contains(session.status()))throw new BusinessException(409,"上传会话当前不能合并");}
    private Map<String,Object> view(Session session){List<Integer> received="COMPLETED".equals(session.status())?new ArrayList<>():jdbc.sql("SELECT chunk_index FROM file_upload_chunk WHERE upload_id=:id ORDER BY chunk_index").param("id",session.id()).query(Integer.class).list();if("COMPLETED".equals(session.status()))for(int i=0;i<session.totalChunks();i++)received.add(i);List<Integer> missing=new ArrayList<>();for(int i=0;i<session.totalChunks();i++)if(!received.contains(i))missing.add(i);Map<String,Object> value=new LinkedHashMap<>();value.put("uploadId",session.id());value.put("status",session.status());value.put("originalName",session.name());value.put("sizeBytes",session.sizeBytes());value.put("sha256",session.sha256());value.put("chunkSize",session.chunkSize());value.put("totalChunks",session.totalChunks());value.put("uploadedChunks",received);value.put("missingChunks",missing);value.put("expiresTime",session.expiresTime());value.put("fileId",session.fileAssetId());return value;}
    private Map<String,Object> completedView(Session session,long fileId){Map<String,Object> value=new LinkedHashMap<>();List<Integer> received=new ArrayList<>();for(int i=0;i<session.totalChunks();i++)received.add(i);value.put("uploadId",session.id());value.put("status","COMPLETED");value.put("originalName",session.name());value.put("sizeBytes",session.sizeBytes());value.put("sha256",session.sha256());value.put("chunkSize",session.chunkSize());value.put("totalChunks",session.totalChunks());value.put("uploadedChunks",received);value.put("missingChunks",List.of());value.put("expiresTime",session.expiresTime());value.put("fileId",fileId);return value;}
    private void resetAfterFailure(UUID id){jdbc.sql("UPDATE file_upload_session SET status='FAILED',updated_time=now() WHERE id=:id AND status='COMPLETING'").param("id",id).update();}
    private void cleanupChunks(UUID id){List<String> values=jdbc.sql("SELECT gridfs_id FROM file_upload_chunk WHERE upload_id=:id").param("id",id).query(String.class).list();for(String value:values)try{gridFs.delete(Query.query(Criteria.where("_id").is(new ObjectId(value))));}catch(Exception ignored){}jdbc.sql("DELETE FROM file_upload_chunk WHERE upload_id=:id").param("id",id).update();}
    private String safeFilename(String value){String name=value==null?"file":value.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\r\\n\\u0000-\\u001f]","_");return name.isBlank()?"file":name.substring(0,Math.min(name.length(),255));}
    private String safeError(Exception ex){String value=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();return value.substring(0,Math.min(300,value.length()));}
    private Session sessionRow(ResultSet rs,int n)throws SQLException{return new Session(rs.getObject("id",UUID.class),rs.getString("original_name"),rs.getString("content_type"),rs.getLong("size_bytes"),rs.getString("expected_sha256"),rs.getString("business_type"),rs.getString("business_ref"),rs.getLong("data_scope_id"),rs.getInt("chunk_size"),rs.getInt("total_chunks"),rs.getString("status"),rs.getLong("uploaded_by"),(Long)rs.getObject("file_asset_id"),rs.getObject("expires_time",java.time.OffsetDateTime.class).toInstant());}

    public record Initiate(UUID uploadId,String originalName,String contentType,long sizeBytes,String sha256,String businessType,String businessRef,long dataScopeId){}
    private record Session(UUID id,String name,String contentType,long sizeBytes,String sha256,String businessType,String businessRef,long dataScopeId,int chunkSize,int totalChunks,String status,long uploadedBy,Long fileAssetId,Instant expiresTime){}
    private record Chunk(int chunkIndex,int sizeBytes,String sha256,String gridfsId){}
    private static final class CountingInputStream extends FilterInputStream {private final long maximum;private long count;private CountingInputStream(InputStream in,long maximum){super(in);this.maximum=maximum;}@Override public int read()throws IOException{int value=super.read();if(value>=0&&++count>maximum)throw new IOException("分片超过声明大小");return value;}@Override public int read(byte[] buffer,int offset,int length)throws IOException{int read=super.read(buffer,offset,Math.min(length,(int)Math.min(Integer.MAX_VALUE,maximum-count+1)));if(read>0&&((count+=read)>maximum))throw new IOException("分片超过声明大小");return read;}long count(){return count;}}
    private final class ChunkSequenceInputStream extends InputStream {private final java.util.Iterator<Chunk> iterator;private InputStream current;private ChunkSequenceInputStream(List<Chunk> chunks){this.iterator=chunks.iterator();}private boolean advance()throws IOException{if(current!=null){current.close();current=null;}if(!iterator.hasNext())return false;Chunk chunk=iterator.next();GridFSFile stored=gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(chunk.gridfsId()))));if(stored==null)throw new IOException("上传分片暂不可用");current=gridFs.getResource(stored).getInputStream();return true;}@Override public int read()throws IOException{while(current!=null||advance()){int value=current.read();if(value>=0)return value;current.close();current=null;}return-1;}@Override public int read(byte[] buffer,int offset,int length)throws IOException{while(current!=null||advance()){int read=current.read(buffer,offset,length);if(read>=0)return read;current.close();current=null;}return-1;}@Override public void close()throws IOException{if(current!=null)current.close();}}
}
