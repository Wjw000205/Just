package com.justeam.rdp.trace;

import com.justeam.rdp.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trace")
public class TraceController {
    private final TraceService service;
    public TraceController(TraceService service) { this.service = service; }

    @GetMapping("/entities")
    @PreAuthorize("hasAuthority('trace:read')")
    public ApiResponse<List<Map<String, Object>>> entities(@RequestParam(required=false) String keyword,
                                                            @RequestParam(required=false) String type) {
        return ApiResponse.ok(service.entities(keyword, type));
    }

    @PostMapping("/entities")
    @PreAuthorize("hasAuthority('trace:write')")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody EntityBody body) {
        long id = service.create(new TraceService.EntityBody(body.entityType(), body.entityCode(), body.entityName(),
                body.properties(), body.dataScopeId(), body.sourceSystem(), body.sourceRecordId()));
        return ApiResponse.ok("追溯实体已创建", Map.of("id", id));
    }

    @PostMapping("/relations")
    @PreAuthorize("hasAuthority('trace:write')")
    public ApiResponse<Map<String, Long>> relate(@Valid @RequestBody RelationBody body) {
        long id = service.relate(new TraceService.RelationBody(body.fromEntityId(), body.toEntityId(), body.relationType(),
                body.properties(), body.effectiveTime()));
        return ApiResponse.ok("追溯关系已创建", Map.of("id", id));
    }

    @GetMapping("/graph/{id}")
    @PreAuthorize("hasAuthority('trace:read')")
    public ApiResponse<Map<String, Object>> graph(@PathVariable long id,
            @RequestParam(defaultValue="both") String direction, @RequestParam(defaultValue="6") int maxDepth) {
        return ApiResponse.ok(service.graph(id, direction, maxDepth));
    }

    @GetMapping("/entities/{id}/evidence")
    @PreAuthorize("hasAuthority('trace:read')")
    public ApiResponse<Map<String, Object>> evidence(@PathVariable @Positive long id) {
        return ApiResponse.ok(service.evidence(id));
    }

    public record EntityBody(@NotBlank(message="实体类型不能为空") String entityType,
                             @NotBlank(message="实体编码不能为空") String entityCode,
                             @NotBlank(message="实体名称不能为空") String entityName,
                             @NotNull(message="属性不能为空") Map<String,Object> properties,
                             @Positive(message="数据域不正确") long dataScopeId,
                             String sourceSystem, String sourceRecordId) {}
    public record RelationBody(@Positive long fromEntityId, @Positive long toEntityId,
                               @NotBlank(message="关系类型不能为空") String relationType,
                               @NotNull Map<String,Object> properties, Instant effectiveTime) {}
}
