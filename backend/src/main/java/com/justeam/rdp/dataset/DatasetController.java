package com.justeam.rdp.dataset;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final DatasetService service;
    private final DatasetImportService imports;
    private final StepUpService stepUp;
    private final DatasetExportPackageService packages;
    private final DatasetExportTicketService exportTickets;

    public DatasetController(DatasetService service,DatasetImportService imports,StepUpService stepUp,DatasetExportPackageService packages,DatasetExportTicketService exportTickets) { this.service = service;this.imports=imports;this.stepUp=stepUp;this.packages=packages;this.exportTickets=exportTickets; }

    @GetMapping
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.list(blank(keyword), blank(category), status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('dataset:create')")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody Body body) {
        return ApiResponse.ok("数据集已创建", Map.of("id", service.create(body.serviceBody())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('dataset:update')")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody Body body) {
        service.update(id, body.serviceBody()); return ApiResponse.ok("数据集已更新", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dataset:delete')")
    public ApiResponse<Void> delete(@PathVariable long id,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request) {
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.DELETE,request);service.delete(id);return ApiResponse.ok("数据集已删除", null); }

    @PutMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<Map<String, Boolean>> favorite(@PathVariable long id) {
        service.favorite(id); return ApiResponse.ok(Map.of("favorited", true));
    }

    @DeleteMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<Map<String, Boolean>> unfavorite(@PathVariable long id) {
        service.unfavorite(id); return ApiResponse.ok(Map.of("favorited", false));
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<PageResponse<Map<String, Object>>> records(@PathVariable long id,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.records(id, pageNum, pageSize));
    }

    @GetMapping("/{id}/records/{recordId}")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<Map<String,Object>> record(@PathVariable long id,@PathVariable String recordId){
        return ApiResponse.ok(service.record(id,recordId));
    }

    @PostMapping("/{id}/records")
    @PreAuthorize("hasAuthority('dataset:create')")
    public ApiResponse<Map<String, Object>> createRecord(@PathVariable long id,
            @RequestHeader("X-Idempotency-Key") java.util.UUID idempotencyKey,
            @RequestBody Map<String, Object> data) {
        return ApiResponse.ok("记录已保存", service.createRecord(id, idempotencyKey, data));
    }

    @PutMapping("/{id}/records/{recordId}")
    @PreAuthorize("hasAuthority('dataset:update')")
    public ApiResponse<Map<String, Object>> updateRecord(@PathVariable long id, @PathVariable String recordId,
            @RequestParam int version, @RequestHeader("X-Idempotency-Key") java.util.UUID idempotencyKey,
            @Valid @RequestBody CorrectionBody body) {
        return ApiResponse.ok(service.updateRecord(id, recordId, version, body.reason(), idempotencyKey, body.data()));
    }

    @DeleteMapping("/{id}/records/{recordId}")
    @PreAuthorize("hasAuthority('dataset:delete')")
    public ApiResponse<Void> deleteRecord(@PathVariable long id, @PathVariable String recordId, @RequestParam int version,
            @RequestParam @NotBlank(message = "删除原因不能为空") String reason,
            @RequestHeader("X-Idempotency-Key") java.util.UUID idempotencyKey,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request) {
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.DELETE,request);
        service.deleteRecord(id, recordId, version, reason, idempotencyKey); return ApiResponse.ok("记录已受控删除", null);
    }

    @GetMapping("/{id}/download-fields")
    @PreAuthorize("hasAuthority('dataset:read')")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> fields(@PathVariable long id) {
        return ApiResponse.ok(Map.of("fields",service.exportFieldDescriptors(service.get(id))));
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAuthority('dataset:export')")
    public void export(@PathVariable long id,@RequestParam(defaultValue="csv")String format,
                                                         @RequestParam(required=false)List<String> fields,
                                                         @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request,
                                                         jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException {
        var actor=CurrentUser.require();Map<String,Object> dataset;String normalized=format.toLowerCase(java.util.Locale.ROOT);try{stepUp.consume(actor,proof,StepUpService.Purpose.EXPORT,request);dataset=service.get(id);service.requireOperation(dataset,"EXPORT");if(!java.util.Set.of("csv","json","xlsx").contains(normalized))throw com.justeam.rdp.common.BusinessException.badRequest("导出格式仅支持 csv、json、xlsx");service.validateExportFields(dataset,fields);}catch(RuntimeException ex){service.auditExportPreflightFailure(id,format,fields,actor,ex);throw ex;}
        String name = dataset.get("name") + "."+normalized;boolean includeUnpublished=service.canAudit(id);
        String media=switch(normalized){case "json"->"application/json;charset=UTF-8";case "xlsx"->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";default->"text/csv;charset=UTF-8";};
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+URLEncoder.encode(name,StandardCharsets.UTF_8));noStore(response);response.setContentType(media);service.export(id,dataset,normalized,fields,response.getOutputStream(),actor.id(),actor.username(),includeUnpublished);
    }

    @GetMapping("/{id}/export-package")
    @PreAuthorize("hasAuthority('dataset:export') and hasAuthority('file:read')")
    public void exportPackage(@PathVariable long id,@RequestParam(defaultValue="csv")String format,
                                                                @RequestParam(required=false)List<String> fields,
                                                                @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request,
                                                                jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException{
        var actor=CurrentUser.require();Map<String,Object> dataset;String normalized=format.toLowerCase(java.util.Locale.ROOT);try{stepUp.consume(actor,proof,StepUpService.Purpose.EXPORT,request);dataset=service.get(id);service.requireOperation(dataset,"EXPORT");if(!java.util.Set.of("csv","json","xlsx").contains(normalized))throw com.justeam.rdp.common.BusinessException.badRequest("导出格式仅支持 csv、json、xlsx");service.validateExportFields(dataset,fields);packages.preflight(id);}catch(RuntimeException ex){service.auditExportPreflightFailure(id,format,fields,actor,ex);throw ex;}
        boolean includeUnpublished=service.canAudit(id);String name=dataset.get("name")+"-含附件.zip";response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+URLEncoder.encode(name,StandardCharsets.UTF_8));noStore(response);response.setContentType("application/zip");packages.export(id,dataset,normalized,fields,response.getOutputStream(),actor,includeUnpublished);
    }

    @PostMapping("/{id}/exports")
    @PreAuthorize("hasAuthority('dataset:export')")
    public ApiResponse<Map<String,Object>> issueExport(@PathVariable long id,@Valid @RequestBody ExportBody body,
                                                        @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        var actor=CurrentUser.require();String format=body.format().toLowerCase(java.util.Locale.ROOT);try{stepUp.consume(actor,proof,StepUpService.Purpose.EXPORT,request,body);Map<String,Object> dataset=service.get(id);service.requireOperation(dataset,"EXPORT");if(!java.util.Set.of("csv","json","xlsx").contains(format))throw com.justeam.rdp.common.BusinessException.badRequest("导出格式仅支持 csv、json、xlsx");service.validateExportFields(dataset,body.fields());if(body.includeAttachments()){if(!actor.permissions().contains("file:read"))throw com.justeam.rdp.common.BusinessException.forbidden("附件压缩包需要附件读取权限");packages.preflight(id);}var issued=exportTickets.issue(actor,new DatasetExportTicketService.ExportRequest(id,format,body.fields(),body.includeAttachments()),request.getRemoteAddr());return ApiResponse.ok("一次性下载地址已生成",Map.of("downloadUrl","/api/datasets/export-download/"+issued.token(),"expiresIn",issued.expiresIn()));}catch(RuntimeException ex){service.auditExportPreflightFailure(id,format,body.fields(),actor,ex);throw ex;}
    }

    @GetMapping("/export-download/{ticket}")
    public void downloadExport(@PathVariable String ticket,HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException{
        var authorized=exportTickets.consume(ticket,request.getRemoteAddr());var user=authorized.user();var previous=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));
        try{var export=authorized.request();Map<String,Object> dataset=service.get(export.datasetId());service.requireOperation(dataset,"EXPORT");service.validateExportFields(dataset,export.fields());if(export.includeAttachments())packages.preflight(export.datasetId());boolean includeUnpublished=service.canAudit(export.datasetId());String filename=String.valueOf(dataset.get("name"))+(export.includeAttachments()?"-含附件.zip":"."+export.format());response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+URLEncoder.encode(filename,StandardCharsets.UTF_8));response.setHeader("Referrer-Policy","no-referrer");noStore(response);response.setContentType(export.includeAttachments()?"application/zip":switch(export.format()){case "json"->"application/json;charset=UTF-8";case "xlsx"->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";default->"text/csv;charset=UTF-8";});if(export.includeAttachments())packages.export(export.datasetId(),dataset,export.format(),export.fields(),response.getOutputStream(),user,includeUnpublished);else service.export(export.datasetId(),dataset,export.format(),export.fields(),response.getOutputStream(),user.id(),user.username(),includeUnpublished);}catch(RuntimeException ex){service.auditExportPreflightFailure(authorized.request().datasetId(),authorized.request().format(),authorized.request().fields(),user,ex);throw ex;}finally{if(previous==null)org.springframework.security.core.context.SecurityContextHolder.clearContext();else org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);}
    }

    @PostMapping(value="/{id}/imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('dataset:import')")
    public ApiResponse<Map<String,Object>> importFile(@PathVariable long id,@RequestPart org.springframework.web.multipart.MultipartFile file){return ApiResponse.ok("导入作业已完成",imports.importFile(id,file));}

    @GetMapping("/{id}/imports/template")
    @PreAuthorize("hasAuthority('dataset:import')")
    public void importTemplate(@PathVariable long id,@RequestParam(defaultValue="xlsx")String format,
                               jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException{
        Map<String,Object> dataset=service.get(id);service.requireWriteAccess(dataset);String normalized=format.toLowerCase(java.util.Locale.ROOT);
        if(!java.util.Set.of("csv","json","xlsx").contains(normalized))throw com.justeam.rdp.common.BusinessException.badRequest("导入模板仅支持 csv、json、xlsx");
        String filename=String.valueOf(dataset.get("name"))+"-导入模板."+normalized;
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+URLEncoder.encode(filename,StandardCharsets.UTF_8));noStore(response);
        response.setContentType(switch(normalized){case "json"->"application/json;charset=UTF-8";case "xlsx"->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";default->"text/csv;charset=UTF-8";});
        imports.writeTemplate(dataset,normalized,response.getOutputStream());
    }

    @GetMapping("/{id}/imports")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<List<Map<String,Object>>> importJobs(@PathVariable long id){return ApiResponse.ok(imports.jobs(id));}

    @GetMapping("/imports/{jobId}")
    @PreAuthorize("hasAuthority('dataset:read')")
    public ApiResponse<Map<String,Object>> importJob(@PathVariable long jobId){return ApiResponse.ok(imports.job(jobId));}

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void noStore(jakarta.servlet.http.HttpServletResponse response){response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store, private, max-age=0");response.setHeader(HttpHeaders.PRAGMA,"no-cache");response.setHeader(HttpHeaders.EXPIRES,"0");}

    public record Body(@NotBlank(message = "数据集名称不能为空") @Size(max = 200, message = "数据集名称最长200字符") String name,
                       String description, String category, String tags,Long scientificCategoryId,Long industryCategoryId,
                       @NotEmpty(message = "字段定义不能为空") List<Map<String, Object>> fieldDefinition,
                       @NotNull(message = "请选择数据域") @Positive(message = "数据域不正确") Long dataScopeId,
                       Long templateId) {
        DatasetService.DatasetBody serviceBody() {
            return new DatasetService.DatasetBody(name, description, category, tags,scientificCategoryId,industryCategoryId, fieldDefinition, dataScopeId, templateId);
        }
    }

    public record CorrectionBody(@NotBlank(message = "更正原因不能为空") String reason,
                                 @NotNull(message = "更正数据不能为空") Map<String, Object> data) {}
    public record ExportBody(@NotBlank String format,@NotEmpty List<String> fields,boolean includeAttachments){}
}
