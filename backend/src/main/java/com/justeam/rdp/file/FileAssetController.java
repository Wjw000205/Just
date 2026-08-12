package com.justeam.rdp.file;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/files")
public class FileAssetController {
    private static final java.util.Set<String> SAFE_PREVIEW_TYPES=java.util.Set.of("application/pdf","text/plain","image/png","image/jpeg","image/gif","image/webp","image/bmp");
    private final FileAssetService service;
    private final StepUpService stepUp;
    public FileAssetController(FileAssetService service,StepUpService stepUp){this.service=service;this.stepUp=stepUp;}

    @GetMapping
    @PreAuthorize("hasAuthority('file:read')")
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam String businessType,@RequestParam String businessRef){return ApiResponse.ok(service.list(businessType,businessRef));}

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<Map<String,Long>> upload(@RequestPart MultipartFile file,@RequestParam String businessType,
            @RequestParam String businessRef,@RequestParam @Positive long dataScopeId){return ApiResponse.ok("文件已上传",Map.of("id",service.upload(file,businessType,businessRef,dataScopeId)));}

    @PostMapping(path="/batch",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<Map<String,Object>> uploadBatch(@RequestPart("files") List<MultipartFile> files,
            @RequestParam String businessType,@RequestParam String businessRef,@RequestParam @Positive long dataScopeId){
        if(files==null||files.isEmpty())throw com.justeam.rdp.common.BusinessException.badRequest("请选择至少一个文件");
        if(files.size()>20)throw com.justeam.rdp.common.BusinessException.badRequest("单批最多上传20个文件");
        List<Map<String,Object>> results=new java.util.ArrayList<>();int success=0;
        for(MultipartFile file:files){Map<String,Object> result=new java.util.LinkedHashMap<>();result.put("originalName",file==null?"":file.getOriginalFilename());
            try{long id=service.upload(file,businessType,businessRef,dataScopeId);result.put("status","SUCCESS");result.put("id",id);success++;}
            catch(com.justeam.rdp.common.BusinessException ex){result.put("status","FAILED");result.put("error",ex.getMessage());}
            catch(Exception ex){result.put("status","FAILED");result.put("error","文件上传失败");}
            results.add(result);}
        Map<String,Object> summary=new java.util.LinkedHashMap<>();summary.put("totalCount",files.size());summary.put("successCount",success);summary.put("failureCount",files.size()-success);summary.put("files",results);
        return ApiResponse.ok(success==files.size()?"批量上传完成":"批量上传部分完成",summary);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('file:read')")
    public void download(@PathVariable long id,@RequestParam(defaultValue="false") boolean preview,
                         @RequestHeader(value=HttpHeaders.RANGE,required=false)String range,
                         jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException{
        FileAssetService.AssetContent content=service.open(id);Map<String,Object> meta=content.metadata();
        String type=String.valueOf(meta.get("contentType"));boolean safePreview=preview&&SAFE_PREVIEW_TYPES.contains(type.toLowerCase(java.util.Locale.ROOT));
        ContentDisposition disposition=(safePreview?ContentDisposition.inline():ContentDisposition.attachment())
                .filename(String.valueOf(meta.get("originalName")),StandardCharsets.UTF_8).build();
        long size=((Number)meta.get("sizeBytes")).longValue();
        long start=0,end=size-1;if(range!=null&&!range.isBlank()){long[] bounds;try{bounds=parseRange(range,size);}catch(com.justeam.rdp.common.BusinessException ex){response.setHeader(HttpHeaders.CONTENT_RANGE,"bytes */"+size);throw ex;}start=bounds[0];end=bounds[1];response.setStatus(206);response.setHeader(HttpHeaders.CONTENT_RANGE,"bytes "+start+"-"+end+"/"+size);}
        response.setContentType(type);response.setContentLengthLong(end-start+1);response.setHeader(HttpHeaders.ACCEPT_RANGES,"bytes");response.setHeader(HttpHeaders.CONTENT_DISPOSITION,disposition.toString());response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store, private, max-age=0");response.setHeader(HttpHeaders.PRAGMA,"no-cache");response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Content-Security-Policy","default-src 'none'; sandbox");
        try(var input=content.resource().getInputStream()){if(start>0)input.skipNBytes(start);byte[] buffer=new byte[64*1024];long remaining=end-start+1;while(remaining>0){int read=input.read(buffer,0,(int)Math.min(buffer.length,remaining));if(read<0)break;response.getOutputStream().write(buffer,0,read);remaining-=read;}response.getOutputStream().flush();}
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('file:delete')")
    public ApiResponse<Void> delete(@PathVariable long id,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.DELETE,request);service.delete(id);return ApiResponse.ok("文件已删除",null);}

    private long[] parseRange(String header,long size){
        if(size<=0||!header.matches("^bytes=\\d*-\\d*$")||header.contains(","))throw new com.justeam.rdp.common.BusinessException(416,"Range范围不正确");
        String[] parts=header.substring(6).split("-",-1);long start,end;
        try{if(parts[0].isBlank()){long suffix=Long.parseLong(parts[1]);if(suffix<=0)throw new NumberFormatException();start=Math.max(0,size-suffix);end=size-1;}else{start=Long.parseLong(parts[0]);end=parts[1].isBlank()?size-1:Long.parseLong(parts[1]);}}catch(Exception ex){throw new com.justeam.rdp.common.BusinessException(416,"Range范围不正确");}
        if(start<0||start>=size||end<start){throw new com.justeam.rdp.common.BusinessException(416,"Range范围不正确");}return new long[]{start,Math.min(end,size-1)};
    }
}
