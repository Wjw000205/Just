package com.justeam.rdp.dashboard;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    private final StepUpService stepUp;
    public DashboardController(DashboardService service,StepUpService stepUp){this.service=service;this.stepUp=stepUp;}
    @GetMapping @PreAuthorize("hasAuthority('dashboard:read')")
    public ApiResponse<Map<String,Object>> overview(){return ApiResponse.ok(service.overview());}
    @GetMapping("/report") @PreAuthorize("hasAuthority('dashboard:read')")
    public ApiResponse<Map<String,Object>> report(){return ApiResponse.ok(service.decisionReport());}
    @GetMapping("/report/export") @PreAuthorize("hasAuthority('dashboard:read') and hasAuthority('dataset:export')")
    public org.springframework.http.ResponseEntity<byte[]> exportReport(@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.EXPORT,request);byte[] bytes=service.reportCsv();return org.springframework.http.ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=decision-report.csv").body(bytes);}
    @GetMapping("/search") @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String,Object>> search(@RequestParam String keyword){return ApiResponse.ok(service.search(keyword));}
}
