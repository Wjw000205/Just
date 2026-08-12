package com.justeam.rdp.file;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.DataScopeService;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.sharing.SharingService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FileAssetService {
    private static final Set<String> BUSINESS_TYPES = Set.of("TEMPLATE","DATASET","DATASET_RECORD","TRACE_ENTITY","DEVICE","OTHER");
    private final JdbcClient jdbc;
    private final MongoTemplate mongo;
    private final GridFsTemplate gridFs;
    private final DataScopeService scopes;
    private final AuditService audit;
    private final boolean requireVirusScan;
    private final TransactionTemplate transactions;
    private final SharingService sharing;

    public FileAssetService(JdbcClient jdbc, MongoTemplate mongo, GridFsTemplate gridFs, DataScopeService scopes, AuditService audit,
                            @Value("${rdp.file.require-virus-scan:false}") boolean requireVirusScan,TransactionTemplate transactions,SharingService sharing) {
        this.jdbc=jdbc;this.mongo=mongo;this.gridFs=gridFs;this.scopes=scopes;this.audit=audit;this.requireVirusScan=requireVirusScan;this.transactions=transactions;this.sharing=sharing;
    }

    public List<Map<String,Object>> list(String businessType,String businessRef) {
        String type=normalizeType(businessType); String ref=normalizeRef(type,businessRef);
        UserPrincipal user=CurrentUser.require();
        Long bindingScope="OTHER".equals(type)?null:bindingScope(type,ref,false,"READ");
        String scope=bindingScope!=null?"f.data_scope_id=:bindingScope":user.admin()?"TRUE":user.dataScopes().isEmpty()?"FALSE":"f.data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec spec=jdbc.sql("""
                SELECT f.* FROM file_asset f WHERE f.status='AVAILABLE'
                AND f.business_type=:type AND f.business_ref=:ref AND 
                """+scope+" ORDER BY f.created_time DESC LIMIT 500").param("type",type).param("ref",ref);
        if(bindingScope!=null)spec=spec.param("bindingScope",bindingScope);else if(!user.admin()&&!user.dataScopes().isEmpty())spec=spec.param("scopes",user.dataScopes());
        return spec.query(this::row).list().stream().map(this::publicView).toList();
    }

    public Map<String,Object> get(long id) {
        return publicView(internal(id,"READ"));
    }

    @Transactional
    public long upload(MultipartFile file,String businessType,String businessRef,long dataScopeId) {
        if(file==null||file.isEmpty())throw BusinessException.badRequest("请选择非空文件");
        try(InputStream input=file.getInputStream()){
            return storeStream(input,file.getSize(),null,file.getOriginalFilename(),file.getContentType(),businessType,businessRef,dataScopeId);
        }catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException(500,"文件保存失败");}
    }

    public UploadTarget validateUploadTarget(String businessType,String businessRef,long dataScopeId){
        if(requireVirusScan) throw new BusinessException(503,"生产病毒扫描被设为必需，但尚未配置扫描适配器");
        String type=normalizeType(businessType),ref=normalizeRef(type,businessRef);
        scopes.require(dataScopeId);validateBinding(type,ref,dataScopeId);
        return new UploadTarget(type,ref,dataScopeId);
    }

    @Transactional
    public long storeStream(InputStream input,long expectedSize,String expectedSha256,String originalName,String rawContentType,
                            String businessType,String businessRef,long dataScopeId) {
        if(input==null||expectedSize<=0)throw BusinessException.badRequest("请选择非空文件");
        UploadTarget target=validateUploadTarget(businessType,businessRef,dataScopeId);
        String filename=safeFilename(originalName);
        String contentType=rawContentType==null||rawContentType.isBlank()?"application/octet-stream":rawContentType;
        try{contentType=org.springframework.http.MediaType.parseMediaType(contentType).toString();}catch(Exception ex){throw BusinessException.badRequest("文件内容类型不正确");}
        ObjectId gridId=null;
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            CountingInputStream counted=new CountingInputStream(input);
            DigestInputStream signed=new DigestInputStream(counted,digest);
            Document metadata=new Document("businessType",target.businessType()).append("businessRef",target.businessRef()).append("dataScopeId",dataScopeId);
            gridId=gridFs.store(signed,filename,contentType,metadata);
            ObjectId rollbackGridId=gridId;
            if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status==TransactionSynchronization.STATUS_ROLLED_BACK)try{gridFs.delete(Query.query(Criteria.where("_id").is(rollbackGridId)));}catch(Exception ignored){}}});
            String sha256=HexFormat.of().formatHex(digest.digest());
            if(counted.count()!=expectedSize)throw BusinessException.badRequest("文件大小与声明不一致");
            if(expectedSha256!=null&&!expectedSha256.equalsIgnoreCase(sha256))throw BusinessException.badRequest("文件整体SHA-256校验失败");
            UserPrincipal user=CurrentUser.require();
            Long id=jdbc.sql("""
                    INSERT INTO file_asset(original_name,content_type,size_bytes,sha256,gridfs_id,business_type,business_ref,
                                           data_scope_id,scan_status,uploaded_by,uploaded_by_name)
                    VALUES (:name,:contentType,:size,:sha256,:gridId,:type,:ref,:scope,'NOT_CONFIGURED',:userId,:userName)
                    RETURNING id
                    """).param("name",filename).param("contentType",contentType).param("size",counted.count())
                    .param("sha256",sha256).param("gridId",gridId.toHexString()).param("type",target.businessType()).param("ref",target.businessRef())
                    .param("scope",dataScopeId).param("userId",user.id()).param("userName",user.realName()).query(Long.class).single();
            audit.record("UPLOAD","FILE","上传业务附件",Map.of("fileId",id,"name",filename,"sha256",sha256,"sizeBytes",counted.count(),"businessType",target.businessType(),"businessRef",target.businessRef()));
            return id;
        }catch(BusinessException ex){if(gridId!=null)gridFs.delete(Query.query(Criteria.where("_id").is(gridId)));throw ex;
        }catch(Exception ex){if(gridId!=null)gridFs.delete(Query.query(Criteria.where("_id").is(gridId)));throw new BusinessException(500,"文件保存失败");}
    }

    public AssetContent open(long id) {
        Map<String,Object> asset=internal(id,"DOWNLOAD");
        ObjectId gridId;
        try{gridId=new ObjectId(String.valueOf(asset.get("gridFsId")));}catch(Exception ex){throw new BusinessException(500,"文件存储标识损坏");}
        GridFSFile stored=gridFs.findOne(Query.query(Criteria.where("_id").is(gridId)));
        if(stored==null)throw new BusinessException(503,"文件内容暂不可用，请联系管理员执行一致性检查");
        audit.record("DOWNLOAD","FILE","下载或预览业务附件",Map.of("fileId",id,"name",asset.get("originalName"),
                "sha256",asset.get("sha256"),"businessType",asset.get("businessType"),"businessRef",asset.get("businessRef")));
        return new AssetContent(asset,gridFs.getResource(stored));
    }

    public List<PackageAsset> datasetPackageAssets(long datasetId){
        requireDatasetPackageAccess(datasetId);
        List<Long> ids=jdbc.sql("""
                SELECT id FROM file_asset WHERE status='AVAILABLE' AND
                ((business_type='DATASET' AND business_ref=:dataset) OR
                 (business_type='DATASET_RECORD' AND business_ref LIKE :recordPrefix))
                ORDER BY business_type,business_ref,id
                """).param("dataset",Long.toString(datasetId)).param("recordPrefix",datasetId+":%").query(Long.class).list();
        java.util.ArrayList<PackageAsset> result=new java.util.ArrayList<>();
        for(Long id:ids)try{Map<String,Object> asset=internal(id,"DOWNLOAD");ObjectId gridId=new ObjectId(String.valueOf(asset.get("gridFsId")));GridFSFile stored=gridFs.findOne(Query.query(Criteria.where("_id").is(gridId)));if(stored==null)throw new BusinessException(503,"附件内容暂不可用");result.add(new PackageAsset(asset,gridId.toHexString()));}catch(BusinessException ex){if(ex.code()==404)continue;throw ex;}
        return result;
    }

    public InputStream packageInput(PackageAsset asset)throws IOException{ObjectId id;try{id=new ObjectId(asset.gridFsId());}catch(Exception ex){throw new IOException("附件存储标识损坏",ex);}GridFSFile stored=gridFs.findOne(Query.query(Criteria.where("_id").is(id)));if(stored==null)throw new IOException("附件内容暂不可用");return gridFs.getResource(stored).getInputStream();}

    public void requireDatasetPackageAccess(long datasetId){
        try{bindingScope("DATASET",Long.toString(datasetId),false,"DOWNLOAD");}
        catch(BusinessException ex){throw BusinessException.forbidden("附件压缩包需要数据集附件下载授权");}
    }

    @Transactional
    public void delete(long id) {
        Map<String,Object> asset=internal(id,"WRITE"); UserPrincipal user=CurrentUser.require();
        if("DATASET_RECORD".equals(asset.get("businessType"))){long holds=jdbc.sql("SELECT count(*) FROM lifecycle_hold WHERE resource_type='DATASET_RECORD' AND resource_ref=:ref AND active=TRUE AND (valid_to IS NULL OR valid_to>now())").param("ref",asset.get("businessRef")).query(Long.class).single();if(holds>0)throw new BusinessException(409,"记录处于质量或法律保留，禁止删除证据附件");}
        transactions.executeWithoutResult(status->{int updated=jdbc.sql("UPDATE file_asset SET status='DELETED',deleted_by=:user,deleted_time=now() WHERE id=:id AND status='AVAILABLE'")
                    .param("user",user.id()).param("id",id).update();if(updated==0)throw BusinessException.notFound("文件不存在");
            Map<String,Object> before=publicView(asset),after=new LinkedHashMap<>(before);after.put("status","DELETED");after.put("deletedBy",user.id());
            audit.record("DELETE","FILE","删除业务附件",Map.of("fileId",id,"name",asset.get("originalName"),"sha256",asset.get("sha256"),"before",before,"after",after));});
        Runnable cleanup=()->{try{gridFs.delete(Query.query(Criteria.where("_id").is(new ObjectId(String.valueOf(asset.get("gridFsId"))))));}catch(Exception ex){/* 元数据已不可见；遗留块可由GridFS孤儿清理任务安全回收。 */}};
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){cleanup.run();}});else cleanup.run();
    }

    private Map<String,Object> internal(long id,String operation){UserPrincipal user=CurrentUser.require();Map<String,Object> asset=jdbc.sql("SELECT * FROM file_asset WHERE id=:id AND status='AVAILABLE'").param("id",id).query(this::row).optional().orElseThrow(()->BusinessException.notFound("文件不存在"));try{if("OTHER".equals(asset.get("businessType"))){long scope=((Number)asset.get("dataScopeId")).longValue();if("WRITE".equals(operation))scopes.require(scope);else scopes.requireOperation(scope,operation);}else{long actual=bindingScope(String.valueOf(asset.get("businessType")),String.valueOf(asset.get("businessRef")),"WRITE".equals(operation),operation);if(actual!=((Number)asset.get("dataScopeId")).longValue())throw BusinessException.notFound("文件不存在");}}catch(BusinessException ex){throw BusinessException.notFound("文件不存在");}return asset;}
    private Map<String,Object> publicView(Map<String,Object> asset){Map<String,Object> value=new LinkedHashMap<>(asset);value.remove("gridFsId");return value;}

    private void validateBinding(String type,String ref,long requestedScope) {
        if("OTHER".equals(type))return;
        long actual=bindingScope(type,ref,true,"WRITE");
        if(actual!=requestedScope)throw BusinessException.badRequest("附件数据域必须与关联业务对象一致");
    }
    private long bindingScope(String type,String ref,boolean write,String operation){
        if("DATASET_RECORD".equals(type))return datasetRecordScope(ref,write,operation);
        if("TEMPLATE".equals(type))return templateScope(ref,write);
        String table=switch(type){case "DATASET"->"data_dataset";case "TRACE_ENTITY"->"trace_entity";case "DEVICE"->"device";default->throw BusinessException.badRequest("不支持的业务类型");};long id;try{id=Long.parseLong(ref);}catch(NumberFormatException ex){throw BusinessException.badRequest("业务对象标识必须为数字");}long scope=jdbc.sql("SELECT data_scope_id FROM "+table+" WHERE id=:id"+("DATASET".equals(type)||"TRACE_ENTITY".equals(type)?" AND deleted=0":"")).param("id",id).query(Long.class).optional().orElseThrow(()->BusinessException.notFound("关联业务对象不存在"));if(write)scopes.require(scope);else if("DATASET".equals(type)&&sharing.hasResourceAccess("DATASET",id,operation)){}else scopes.requireOperation(scope,operation);return scope;
    }

    private long templateScope(String ref,boolean write){long id;try{id=Long.parseLong(ref);}catch(NumberFormatException ex){throw BusinessException.badRequest("业务对象标识必须为数字");}Map<String,Object> template=jdbc.sql("SELECT data_scope_id,creator_id,visibility,published,enabled,audit_status FROM tpl_template WHERE id=:id AND deleted=0").param("id",id).query((rs,n)->{Map<String,Object> row=new LinkedHashMap<>();row.put("scope",rs.getLong("data_scope_id"));row.put("creator",rs.getLong("creator_id"));row.put("visibility",rs.getString("visibility"));row.put("published",rs.getBoolean("published"));row.put("enabled",rs.getBoolean("enabled"));row.put("auditStatus",rs.getObject("audit_status"));return row;}).optional().orElseThrow(()->BusinessException.notFound("关联业务对象不存在"));UserPrincipal user=CurrentUser.require();long scope=((Number)template.get("scope")).longValue(),creator=((Number)template.get("creator")).longValue();if(write){if(!user.admin()&&creator!=user.id())throw BusinessException.notFound("关联业务对象不存在");return scope;}boolean publicCatalog="PUBLIC".equals(template.get("visibility"))&&(boolean)template.get("published")&&(boolean)template.get("enabled");boolean auditor=(user.admin()||user.permissions().contains("template:audit"))&&scopes.canWriteAccess(scope)&&"PUBLIC".equals(template.get("visibility"))&&((boolean)template.get("published")||template.get("auditStatus") instanceof Number status&&status.intValue()==0);if(!user.admin()&&creator!=user.id()&&!publicCatalog&&!auditor)throw BusinessException.notFound("关联业务对象不存在");return scope;}

    private long datasetRecordScope(String ref,boolean write,String operation){
        RecordRef record=parseRecordRef(ref);
        long scope=jdbc.sql("SELECT data_scope_id FROM data_dataset WHERE id=:id AND deleted=0")
                .param("id",record.datasetId()).query(Long.class).optional()
                .orElseThrow(()->BusinessException.notFound("关联数据集不存在"));
        if(write){scopes.require(scope);String status=jdbc.sql("SELECT status FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record FOR UPDATE").param("dataset",record.datasetId()).param("record",record.recordId().toHexString()).query(String.class).optional().orElseThrow(()->BusinessException.notFound("关联数据记录不存在"));if(!Set.of("DRAFT","REJECTED").contains(status))throw new BusinessException(409,"记录已提交审核、发布、删除或归档，附件集合已冻结");}else if(sharing.hasResourceAccess("DATASET",record.datasetId(),operation)){}else scopes.requireOperation(scope,operation);
        Document document=mongo.findOne(Query.query(Criteria.where("_id").is(record.recordId()).and("deleted").ne(true).and("archived").ne(true)),
                Document.class,"dataset_data_"+record.datasetId());
        if(document==null)throw BusinessException.notFound("关联数据记录不存在");
        UserPrincipal user=CurrentUser.require();Number owner=document.get("createdBy",Number.class);
        boolean privileged=user.admin()||user.permissions().contains("dataset:audit")&&user.assignedScopes().contains(scope);
        if(!privileged&&(owner==null||owner.longValue()!=user.id())){
            long published=jdbc.sql("SELECT count(*) FROM dataset_record_workflow WHERE dataset_id=:dataset AND record_id=:record AND status='PUBLISHED'")
                    .param("dataset",record.datasetId()).param("record",record.recordId().toHexString()).query(Long.class).single();
            if(published==0)throw BusinessException.notFound("关联数据记录不存在");
        }
        return scope;
    }

    private String normalizeType(String value){String type=value==null?"":value.trim().toUpperCase(java.util.Locale.ROOT);if(!BUSINESS_TYPES.contains(type))throw BusinessException.badRequest("不支持的业务类型");return type;}
    private String normalizeRef(String type,String value){if(value==null||value.isBlank()||value.length()>120)throw BusinessException.badRequest("业务对象标识不能为空且最长120字符");String ref=value.trim();if("DATASET_RECORD".equals(type)){RecordRef record=parseRecordRef(ref);return record.datasetId()+":"+record.recordId().toHexString();}return ref;}
    private RecordRef parseRecordRef(String value){String[] parts=value.split(":",-1);if(parts.length!=2)throw BusinessException.badRequest("数据记录附件标识必须为 数据集ID:记录ID");long datasetId;try{datasetId=Long.parseLong(parts[0]);}catch(Exception ex){throw BusinessException.badRequest("数据集标识不正确");}if(datasetId<=0||!ObjectId.isValid(parts[1]))throw BusinessException.badRequest("数据记录附件标识不正确");return new RecordRef(datasetId,new ObjectId(parts[1]));}
    private String safeFilename(String value){String name=value==null?"file":value.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\r\\n\\u0000-\\u001f]","_");return name.isBlank()?"file":name.substring(0,Math.min(name.length(),255));}
    private Map<String,Object> row(ResultSet rs,int n)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("originalName",rs.getString("original_name"));v.put("contentType",rs.getString("content_type"));v.put("sizeBytes",rs.getLong("size_bytes"));v.put("sha256",rs.getString("sha256"));v.put("gridFsId",rs.getString("gridfs_id"));v.put("businessType",rs.getString("business_type"));v.put("businessRef",rs.getString("business_ref"));v.put("dataScopeId",rs.getLong("data_scope_id"));v.put("scanStatus",rs.getString("scan_status"));v.put("uploadedBy",rs.getLong("uploaded_by"));v.put("uploadedByName",rs.getString("uploaded_by_name"));v.put("createdTime",rs.getObject("created_time"));return v;}
    private record RecordRef(long datasetId,ObjectId recordId){}
    public record AssetContent(Map<String,Object> metadata,GridFsResource resource){}
    public record PackageAsset(Map<String,Object> metadata,String gridFsId){}
    public record UploadTarget(String businessType,String businessRef,long dataScopeId){}
    private static final class CountingInputStream extends FilterInputStream {private long count;private CountingInputStream(InputStream in){super(in);}@Override public int read()throws IOException{int value=super.read();if(value>=0)count++;return value;}@Override public int read(byte[] buffer,int offset,int length)throws IOException{int read=super.read(buffer,offset,length);if(read>0)count+=read;return read;}long count(){return count;}}
}
