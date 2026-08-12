package com.justeam.rdp.audit;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/audits")
public class AuditController {
    private final AuditService service;
    private final StepUpService stepUp;
    public AuditController(AuditService service,StepUpService stepUp) { this.service = service;this.stepUp=stepUp; }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<PageResponse<Map<String, Object>>> list(@RequestParam(required=false) Long userId,
            @RequestParam(required=false) String module, @RequestParam(required=false) String username,
            @RequestParam(required=false) String operation,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) Instant from,
            @RequestParam(required=false) Instant to, @RequestParam(defaultValue="1") int pageNum,
            @RequestParam(defaultValue="20") int pageSize, @RequestParam(defaultValue="DESC") String sortOrder) {
        PageResponse<Map<String,Object>> result=service.list(userId, module, username, operation, keyword, from, to,
                pageNum, pageSize, sortOrder);
        service.record("QUERY", "AUDIT", "查询审计日志", service.queryDetails(userId,module,username,operation,keyword,
                from,to,pageNum,pageSize,sortOrder,result.total()));
        return ApiResponse.ok(result);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('audit:read') and hasAuthority('audit:export')")
    public ResponseEntity<StreamingResponseBody> export(@RequestParam(defaultValue="csv") String format,
            @RequestParam(required=false) Long userId, @RequestParam(required=false) String module,
            @RequestParam(required=false) String username,
            @RequestParam(required=false) String operation, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Instant from, @RequestParam(required=false) Instant to,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request) {
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.EXPORT,request);
        String normalized = format.toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("csv", "xlsx").contains(normalized)) {
            throw com.justeam.rdp.common.BusinessException.badRequest("审计导出仅支持 csv、xlsx");
        }
        String name = "audit-events-" + java.time.LocalDate.now() + "." + normalized;
        String contentType = "xlsx".equals(normalized)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv;charset=UTF-8";
        AuditService.ExportPlan plan = service.prepareExport(normalized, userId, module, username, operation,
                keyword, from, to);
        StreamingResponseBody body = output -> service.export(output, plan);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(name, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType(contentType)).body(body);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<Map<String, Object>> verify() {
        Map<String,Object> result=service.verifyChain();
        service.record("VERIFY", "AUDIT", "验证审计日志摘要链", Map.of("valid",result.get("valid"),
                "verifiedRecords",result.get("verifiedRecords")));
        return ApiResponse.ok(result);
    }
}
