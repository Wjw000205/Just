package com.justeam.rdp.file;

import com.justeam.rdp.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/files/uploads")
@PreAuthorize("hasAuthority('file:upload')")
public class FileResumableUploadController {
    private final FileResumableUploadService service;
    public FileResumableUploadController(FileResumableUploadService service){this.service=service;}

    @PostMapping
    public ApiResponse<Map<String,Object>> initiate(@Valid @RequestBody Body body){return ApiResponse.ok("上传会话已创建",service.initiate(body.value()));}

    @GetMapping("/{uploadId}")
    public ApiResponse<Map<String,Object>> status(@PathVariable UUID uploadId){return ApiResponse.ok(service.status(uploadId));}

    @PutMapping(path="/{uploadId}/chunks/{index}",consumes=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ApiResponse<Map<String,Object>> chunk(@PathVariable UUID uploadId,@PathVariable int index,
                                                  @RequestHeader("X-Chunk-SHA256") String sha256,
                                                  @RequestHeader("Content-Range") String contentRange,
                                                  HttpServletRequest request)throws java.io.IOException{
        return ApiResponse.ok("分片已接收",service.uploadChunk(uploadId,index,sha256,contentRange,request.getInputStream()));
    }

    @PostMapping("/{uploadId}/complete")
    public ApiResponse<Map<String,Object>> complete(@PathVariable UUID uploadId){return ApiResponse.ok("文件上传完成",service.complete(uploadId));}

    @DeleteMapping("/{uploadId}")
    public ApiResponse<Void> cancel(@PathVariable UUID uploadId){service.cancel(uploadId);return ApiResponse.ok("上传已取消",null);}

    public record Body(@NotNull UUID uploadId,
                       @NotBlank @Size(max=255) String originalName,
                       @Size(max=200) String contentType,
                       @Positive long sizeBytes,
                       @NotBlank String sha256,
                       @NotBlank String businessType,
                       @NotBlank @Size(max=120) String businessRef,
                       @Positive long dataScopeId){
        FileResumableUploadService.Initiate value(){return new FileResumableUploadService.Initiate(uploadId,originalName,contentType,sizeBytes,sha256,businessType,businessRef,dataScopeId);}
    }
}
