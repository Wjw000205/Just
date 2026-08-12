package com.justeam.rdp.dataset;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.file.FileAssetService;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DatasetExportPackageService {
    private final DatasetService datasets;
    private final FileAssetService files;
    private final AuditService audit;
    private final JsonSupport json;
    private final Path tempRoot;
    private final long maxPackageBytes;
    private final int maxAttachments;
    private final Semaphore concurrent;
    public DatasetExportPackageService(DatasetService datasets,FileAssetService files,AuditService audit,JsonSupport json,
                                       @Value("${rdp.export.temp-root:./data/export-temp}")String tempRoot,
                                       @Value("${rdp.export.max-package-bytes:5368709120}")long maxPackageBytes,
                                       @Value("${rdp.export.max-attachments:5000}")int maxAttachments,
                                       @Value("${rdp.export.max-concurrent-packages:2}")int maxConcurrent){this.datasets=datasets;this.files=files;this.audit=audit;this.json=json;this.tempRoot=Path.of(tempRoot).toAbsolutePath().normalize();this.maxPackageBytes=Math.max(1048576,maxPackageBytes);this.maxAttachments=Math.max(1,maxAttachments);this.concurrent=new Semaphore(Math.max(1,maxConcurrent));ensureTempRoot();}

    public void preflight(long datasetId){files.requireDatasetPackageAccess(datasetId);}

    public void export(long datasetId,Map<String,Object> dataset,String format,List<String> fields,OutputStream raw,
                       UserPrincipal actor,boolean includeUnpublished)throws IOException{
        Path temp=null;long recordCount=0,attachmentBytes=0;int attachmentCount=0;long packageSize=0;String packageSha="";boolean acquired=false;
        try{
            if(!concurrent.tryAcquire())throw new BusinessException(429,"附件导出任务繁忙，请稍后重试");acquired=true;preflight(datasetId);List<FileAssetService.PackageAsset> assets=files.datasetPackageAssets(datasetId);if(assets.size()>maxAttachments)throw BusinessException.badRequest("附件数量超过单次导出上限");long declared=0;for(FileAssetService.PackageAsset asset:assets){long size=((Number)asset.metadata().get("sizeBytes")).longValue();if(size<0||declared>maxPackageBytes-size)throw BusinessException.badRequest("附件总量超过单次导出上限");declared+=size;verify(asset);}
            ensureTempRoot();temp=Files.createTempFile(tempRoot,"rdp-dataset-export-",".zip");secure(temp);MessageDigest packageDigest=digest();
            try(OutputStream fileOut=new LimitedOutputStream(Files.newOutputStream(temp),maxPackageBytes);CountingOutputStream counted=new CountingOutputStream(fileOut);DigestOutputStream signed=new DigestOutputStream(counted,packageDigest);ZipOutputStream zip=new ZipOutputStream(signed,java.nio.charset.StandardCharsets.UTF_8)){
                ZipEntry dataEntry=new ZipEntry("data."+format);dataEntry.setTime(0);zip.putNextEntry(dataEntry);
                EntryLimitOutputStream dataOutput=new EntryLimitOutputStream(zip,maxPackageBytes);recordCount=datasets.exportForPackage(datasetId,dataset,format,fields,dataOutput,actor.id(),actor.username(),includeUnpublished);dataOutput.flush();zip.closeEntry();
                List<Map<String,Object>> manifestFiles=new ArrayList<>();
                for(FileAssetService.PackageAsset asset:assets){Map<String,Object> meta=asset.metadata();long size=((Number)meta.get("sizeBytes")).longValue();String path=entryPath(datasetId,meta);ZipEntry entry=new ZipEntry(path);entry.setTime(0);zip.putNextEntry(entry);long copied=copy(files.packageInput(asset),zip);zip.closeEntry();if(copied!=size)throw new BusinessException(503,"附件完整性校验失败，压缩包未生成");Map<String,Object> item=new LinkedHashMap<>();item.put("fileId",meta.get("id"));item.put("recordId","DATASET_RECORD".equals(meta.get("businessType"))?String.valueOf(meta.get("businessRef")).substring(String.valueOf(meta.get("businessRef")).indexOf(':')+1):null);item.put("originalName",meta.get("originalName"));item.put("contentType",meta.get("contentType"));item.put("sizeBytes",size);item.put("sha256",meta.get("sha256"));item.put("path",path);manifestFiles.add(item);attachmentCount++;attachmentBytes+=copied;}
                Map<String,Object> manifest=new LinkedHashMap<>();manifest.put("datasetId",datasetId);manifest.put("datasetName",dataset.get("name"));manifest.put("datasetVersion",dataset.get("version"));manifest.put("generatedTime",Instant.now());manifest.put("generatedBy",actor.username());manifest.put("format",format);manifest.put("fields",fields==null||fields.isEmpty()?datasets.exportFieldDescriptors(dataset).stream().map(v->v.get("value")).toList():fields);manifest.put("maskedFields",datasets.exportFieldDescriptors(dataset).stream().filter(v->Boolean.TRUE.equals(v.get("sensitive"))).map(v->v.get("value")).toList());manifest.put("recordCount",recordCount);manifest.put("attachmentCount",attachmentCount);manifest.put("attachmentBytes",attachmentBytes);manifest.put("attachments",manifestFiles);
                ZipEntry manifestEntry=new ZipEntry("attachments-manifest.json");manifestEntry.setTime(0);zip.putNextEntry(manifestEntry);zip.write(json.write(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();zip.finish();signed.flush();packageSize=counted.count();packageSha=HexFormat.of().formatHex(packageDigest.digest());
            }
            try(InputStream verified=Files.newInputStream(temp)){copy(verified,raw);}raw.flush();
            audit.recordAs(actor.id(),actor.username(),"EXPORT_PACKAGE","DATASET","导出数据与附件压缩包",Map.of("datasetId",datasetId,"format",format,"recordCount",recordCount,"attachmentCount",attachmentCount,"attachmentBytes",attachmentBytes,"sizeBytes",packageSize,"sha256",packageSha,"result","COMPLETED"));
        }catch(Exception ex){audit.recordAs(actor.id(),actor.username(),"EXPORT_PACKAGE_FAILED","DATASET","导出数据与附件压缩包失败",Map.of("datasetId",datasetId,"format",format,"recordCount",recordCount,"attachmentCount",attachmentCount,"sizeBytes",packageSize,"errorType",ex.getClass().getSimpleName(),"result","FAILED"));if(ex instanceof IOException io)throw io;if(ex instanceof RuntimeException runtime)throw runtime;throw new IOException(ex);
        }finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ex){audit.recordAs(actor.id(),actor.username(),"EXPORT_TEMP_CLEANUP_FAILED","DATASET","导出临时文件清理失败",Map.of("datasetId",datasetId,"file",temp.getFileName().toString()));}if(acquired)concurrent.release();}
    }

    @Scheduled(initialDelayString="${rdp.export.cleanup-initial-delay-ms:300000}",fixedDelayString="${rdp.export.cleanup-delay-ms:3600000}")
    public void cleanupStale(){ensureTempRoot();Instant cutoff=Instant.now().minus(Duration.ofHours(2));try(var stream=Files.list(tempRoot)){for(Path file:stream.filter(path->path.getFileName().toString().startsWith("rdp-dataset-export-")&&path.getFileName().toString().endsWith(".zip")).toList())try{if(Files.getLastModifiedTime(file).toInstant().isBefore(cutoff))Files.deleteIfExists(file);}catch(Exception ex){audit.recordAs(null,"system","EXPORT_TEMP_CLEANUP_FAILED","DATASET","过期导出临时文件清理失败",Map.of("file",file.getFileName().toString()));}}catch(Exception ex){audit.recordAs(null,"system","EXPORT_TEMP_CLEANUP_FAILED","DATASET","导出临时目录检查失败",Map.of("errorType",ex.getClass().getSimpleName()));}}

    private void ensureTempRoot(){try{Files.createDirectories(tempRoot);secure(tempRoot);}catch(IOException ex){throw new BusinessException(503,"导出临时目录不可用");}}
    private void secure(Path path){try{Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(Files.isDirectory(path)?"rwx------":"rw-------"));}catch(UnsupportedOperationException|IOException ignored){}}

    private void verify(FileAssetService.PackageAsset asset)throws IOException{Map<String,Object> meta=asset.metadata();MessageDigest actual=digest();long copied;try(InputStream input=new DigestInputStream(files.packageInput(asset),actual)){copied=copy(input,OutputStream.nullOutputStream());}if(copied!=((Number)meta.get("sizeBytes")).longValue()||!HexFormat.of().formatHex(actual.digest()).equalsIgnoreCase(String.valueOf(meta.get("sha256"))))throw new BusinessException(503,"附件完整性校验失败，压缩包未生成");}
    private long copy(InputStream input,OutputStream output)throws IOException{try(input){long copied=0;byte[] buffer=new byte[64*1024];for(int read;(read=input.read(buffer))>=0;){if(read==0)continue;output.write(buffer,0,read);copied+=read;}return copied;}}

    private String entryPath(long datasetId,Map<String,Object> meta){String ref=String.valueOf(meta.get("businessRef"));String group="DATASET_RECORD".equals(meta.get("businessType"))?"records/"+safe(ref.substring(ref.indexOf(':')+1)):"dataset";return "attachments/"+group+"/"+meta.get("id")+"-"+safe(String.valueOf(meta.get("originalName")));}
    private String safe(String value){String result=value.replace('\\','/');result=result.substring(result.lastIndexOf('/')+1).replaceAll("[\\r\\n\\u0000-\\u001f]","_").replace("..","_");return result.isBlank()?"file":result.substring(0,Math.min(180,result.length()));}
    private MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static final class CountingOutputStream extends FilterOutputStream {private long count;private CountingOutputStream(OutputStream out){super(out);}@Override public void write(int value)throws IOException{out.write(value);count++;}@Override public void write(byte[] buffer,int offset,int length)throws IOException{out.write(buffer,offset,length);count+=length;}long count(){return count;}}
    private static class LimitedOutputStream extends FilterOutputStream {private final long maximum;private long count;private LimitedOutputStream(OutputStream out,long maximum){super(out);this.maximum=maximum;}@Override public void write(int value)throws IOException{if(count>=maximum)throw new IOException("导出压缩包超过大小上限");out.write(value);count++;}@Override public void write(byte[] buffer,int offset,int length)throws IOException{if(length>maximum-count)throw new IOException("导出压缩包超过大小上限");out.write(buffer,offset,length);count+=length;}}
    private static final class EntryLimitOutputStream extends LimitedOutputStream {private EntryLimitOutputStream(OutputStream out,long maximum){super(out,maximum);}@Override public void close()throws IOException{flush();}}
}
