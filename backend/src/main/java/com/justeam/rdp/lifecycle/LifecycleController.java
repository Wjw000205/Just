package com.justeam.rdp.lifecycle;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lifecycle")
public class LifecycleController {
    private final LifecycleService service;public LifecycleController(LifecycleService service){this.service=service;}
    @GetMapping("/policies") @PreAuthorize("hasAuthority('lifecycle:read')") public ApiResponse<List<Map<String,Object>>> policies(){return ApiResponse.ok(service.policies());}
    @PostMapping("/policies") @PreAuthorize("hasAuthority('lifecycle:manage')") public ApiResponse<Map<String,Long>> createPolicy(@Valid @RequestBody PolicyBody body){return ApiResponse.ok(Map.of("id",service.savePolicy(null,body.importanceLevel(),body.dataScopeId(),body.onlineDays(),body.archiveTarget(),body.autoArchive(),body.active(),null)));}
    @PutMapping("/policies/{id}") @PreAuthorize("hasAuthority('lifecycle:manage')") public ApiResponse<Map<String,Long>> updatePolicy(@PathVariable long id,@Valid @RequestBody PolicyBody body){return ApiResponse.ok(Map.of("id",service.savePolicy(id,body.importanceLevel(),body.dataScopeId(),body.onlineDays(),body.archiveTarget(),body.autoArchive(),body.active(),body.version())));}
    @PutMapping("/datasets/{id}/importance") @PreAuthorize("hasAuthority('lifecycle:manage')") public ApiResponse<Void> importance(@PathVariable long id,@Valid @RequestBody ImportanceBody body){service.setImportance(id,body.importanceLevel());return ApiResponse.ok("数据重要性已更新",null);}
    @GetMapping("/preview") @PreAuthorize("hasAuthority('lifecycle:read')") public ApiResponse<List<Map<String,Object>>> preview(@RequestParam(defaultValue="100") @Min(1) @Max(500) int limit){return ApiResponse.ok(service.preview(limit));}
    @PostMapping("/archive") @PreAuthorize("hasAuthority('lifecycle:archive')") public ApiResponse<Map<String,Object>> archive(@Valid @RequestBody RecordBody body){return ApiResponse.ok("记录已完成校验并归档",service.archive(body.datasetId(),body.recordId()));}
    @GetMapping("/items") @PreAuthorize("hasAuthority('lifecycle:read')") public ApiResponse<PageResponse<Map<String,Object>>> items(@RequestParam(required=false)String state,@RequestParam(required=false) @Size(max=100) String keyword,@RequestParam(defaultValue="1") @Min(1) @Max(1000) int pageNum,@RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize){return ApiResponse.ok(service.items(state,keyword,pageNum,pageSize));}
    @PostMapping("/items/{id}/restore") @PreAuthorize("hasAuthority('lifecycle:restore')") public ApiResponse<Map<String,Object>> restore(@PathVariable long id){return ApiResponse.ok("归档摘要校验通过，记录已恢复",service.restore(id));}
    @PostMapping("/items/{id}/retry") @PreAuthorize("hasAuthority('lifecycle:archive') or hasAuthority('lifecycle:restore')") public ApiResponse<Map<String,Object>> retry(@PathVariable long id){return ApiResponse.ok("失败作业已重试",service.retry(id));}
    @PostMapping("/reconcile") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<Map<String,Integer>> reconcile(){return ApiResponse.ok("陈旧生命周期转换已检查",service.reconcileNow());}
    @GetMapping("/holds") @PreAuthorize("hasAuthority('lifecycle:read')") public ApiResponse<List<Map<String,Object>>> holds(){return ApiResponse.ok(service.holds());}
    @PostMapping("/holds") @PreAuthorize("hasAuthority('lifecycle:hold')") public ApiResponse<Map<String,Long>> hold(@Valid @RequestBody HoldBody body){return ApiResponse.ok(Map.of("id",service.createHold(body.datasetId(),body.recordId(),body.reason(),body.validTo())));}
    @PostMapping("/holds/{id}/release") @PreAuthorize("hasAuthority('lifecycle:hold')") public ApiResponse<Void> release(@PathVariable long id,@Valid @RequestBody ReleaseBody body){service.releaseHold(id,body.version(),body.reason());return ApiResponse.ok("保留例外已解除",null);}

    public record PolicyBody(@NotBlank @Pattern(regexp="LOW|NORMAL|HIGH|CRITICAL") String importanceLevel,Long dataScopeId,@Min(0) @Max(36500) int onlineDays,@NotBlank @Size(max=100)String archiveTarget,boolean autoArchive,boolean active,Integer version){}
    public record ImportanceBody(@NotBlank @Pattern(regexp="LOW|NORMAL|HIGH|CRITICAL")String importanceLevel){}
    public record RecordBody(@Positive long datasetId,@NotBlank @Pattern(regexp="[0-9a-fA-F]{24}")String recordId){}
    public record HoldBody(@Positive long datasetId,@NotBlank @Pattern(regexp="[0-9a-fA-F]{24}")String recordId,@NotBlank @Size(max=1000)String reason,@Future Instant validTo){}
    public record ReleaseBody(@Positive int version,@NotBlank @Size(max=1000)String reason){}
}
