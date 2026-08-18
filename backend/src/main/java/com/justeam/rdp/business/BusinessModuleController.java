package com.justeam.rdp.business;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/business/modules")
public class BusinessModuleController {
    private final BusinessModuleService service;
    private final StepUpService stepUp;

    public BusinessModuleController(BusinessModuleService service, StepUpService stepUp) {
        this.service = service;
        this.stepUp = stepUp;
    }

    @GetMapping("/{module}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> module(@PathVariable String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(service.module(module, keyword, pageNum, pageSize));
    }

    @GetMapping("/{module}/records/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> record(@PathVariable String module, @PathVariable @Positive long id) {
        return ApiResponse.ok(service.record(module, id));
    }

    @PostMapping("/{module}/records")
    @PreAuthorize("hasAuthority('trace:write')")
    public ApiResponse<Map<String, Long>> create(@PathVariable String module, @Valid @RequestBody RecordBody body) {
        long id = service.create(module, body);
        return ApiResponse.ok("业务对象已创建", Map.of("id", id));
    }

    @PutMapping("/{module}/records/{id}")
    @PreAuthorize("hasAuthority('trace:write')")
    public ApiResponse<Void> update(@PathVariable String module, @PathVariable @Positive long id,
            @Valid @RequestBody RecordBody body) {
        service.update(module, id, body);
        return ApiResponse.ok("业务对象已更新", null);
    }

    @DeleteMapping("/{module}/records/{id}")
    @PreAuthorize("hasAuthority('trace:write')")
    public ApiResponse<Void> delete(@PathVariable String module, @PathVariable @Positive long id,
            @RequestParam @Positive int version, @RequestParam @NotBlank String reason,
            @RequestHeader(value = StepUpService.HEADER, required = false) String proof,
            HttpServletRequest request) {
        stepUp.consume(CurrentUser.require(), proof, StepUpService.Purpose.DELETE, request);
        service.delete(module, id, version, reason);
        return ApiResponse.ok("业务对象已删除", null);
    }

    public record RecordBody(@NotBlank(message = "业务编码不能为空") String code,
                             @NotBlank(message = "名称不能为空") String name,
                             String status, String sourceSystem,
                             @Positive(message = "数据域不正确") long dataScopeId,
                             @NotNull(message = "属性不能为空") Map<String, Object> properties,
                             @Positive(message = "版本不正确") Integer version) {
    }
}
