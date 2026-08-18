package com.justeam.rdp.template;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final TemplateService service;
    private final StepUpService stepUp;

    public TemplateController(TemplateService service,StepUpService stepUp) { this.service = service;this.stepUp=stepUp; }

    @GetMapping
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean usable,
            @RequestParam(required = false) String view,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.list(blankToNull(keyword), blankToNull(type), published,
                blankToNull(visibility), enabled, usable, blankToNull(view), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<List<Map<String, Object>>> versions(@PathVariable long id) {
        return ApiResponse.ok(service.versions(id));
    }

    @GetMapping("/{id}/versions/{version}")
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<Map<String, Object>> version(@PathVariable long id, @PathVariable int version) {
        return ApiResponse.ok(service.version(id, version));
    }

    @PostMapping("/{id}/versions/{version}/restore")
    @PreAuthorize("hasAuthority('template:update')")
    public ApiResponse<TemplateService.RestoreResult> restore(@PathVariable long id, @PathVariable int version) {
        return ApiResponse.ok("历史版本已复制为新草稿，原模板及既有引用保持不变", service.restore(id, version));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('template:create')")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody Body body) {
        long id = service.create(body.toService());
        return ApiResponse.ok("模板已创建", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('template:update')")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody Body body) {
        service.update(id, body.toService()); return ApiResponse.ok("模板已更新", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('template:delete')")
    public ApiResponse<Void> delete(@PathVariable long id,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request) {
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.DELETE,request);service.delete(id);return ApiResponse.ok("模板已删除", null); }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('template:publish')")
    public ApiResponse<Void> publish(@PathVariable long id) { service.publish(id); return ApiResponse.ok("已提交审核", null); }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('template:audit')")
    public ApiResponse<Void> audit(@PathVariable long id, @RequestBody AuditBody body) {
        service.audit(id, body.auditStatus(), body.auditComment()); return ApiResponse.ok("审核完成", null);
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('template:audit')")
    public ApiResponse<Void> enabled(@PathVariable long id, @Valid @RequestBody EnabledBody body) {
        service.setEnabled(id, body.enabled(), body.reason());
        return ApiResponse.ok(body.enabled() ? "模板已重新启用" : "模板已停用", null);
    }

    @PutMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<Map<String, Boolean>> favorite(@PathVariable long id) {
        service.favorite(id); return ApiResponse.ok(Map.of("favorited", true));
    }

    @DeleteMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('template:read')")
    public ApiResponse<Map<String, Boolean>> unfavorite(@PathVariable long id) {
        service.unfavorite(id); return ApiResponse.ok(Map.of("favorited", false));
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record Body(@NotBlank(message = "模板名称不能为空") @Size(max = 200, message = "模板名称最长200字符") String name,
                       @NotBlank(message = "模板类型不能为空") @Pattern(regexp = "template|fragment", message = "模板类型不正确") String type,
                       String tag, String description, String source,
                       @NotBlank(message = "模板可见范围必须选择") @Pattern(regexp = "PRIVATE|PUBLIC", message = "模板可见范围不正确") String visibility,
                       @NotNull(message = "模板内容不能为空") Map<String, Object> content,
                       Map<String, Object> schemaDefinition,
                       @NotNull(message = "请选择数据域") @jakarta.validation.constraints.Positive(message = "数据域不正确") Long dataScopeId) {
        TemplateService.TemplateBody toService() {
            return new TemplateService.TemplateBody(name, type, tag, description, source, visibility,
                    content, schemaDefinition, dataScopeId);
        }
    }
    public record AuditBody(int auditStatus, String auditComment) {}
    public record EnabledBody(boolean enabled,
                              @NotBlank(message = "停用或启用原因不能为空") @Size(max = 500) String reason) {}
}
