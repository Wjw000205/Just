package com.justeam.rdp.dataset;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/datasets/{datasetId}/records/{recordId}/workflow")
public class RecordWorkflowController {
    private final RecordWorkflowService service;private final ClientIpResolver clientIps;public RecordWorkflowController(RecordWorkflowService service,ClientIpResolver clientIps){this.service=service;this.clientIps=clientIps;}
    @GetMapping @PreAuthorize("hasAuthority('dataset:read')") public ApiResponse<Map<String,Object>> state(@PathVariable long datasetId,@PathVariable String recordId){return ApiResponse.ok(service.state(datasetId,recordId));}
    @GetMapping("/signatures/{signatureId}/verify") @PreAuthorize("hasAuthority('dataset:read')") public ApiResponse<Map<String,Object>> verifySignature(@PathVariable long datasetId,@PathVariable String recordId,@PathVariable long signatureId){return ApiResponse.ok(service.verifySignature(datasetId,recordId,signatureId));}
    @PostMapping("/submit") @PreAuthorize("hasAuthority('dataset:update')") public ApiResponse<Map<String,Object>> submit(@PathVariable long datasetId,@PathVariable String recordId,@Valid @RequestBody SubmitBody body){return ApiResponse.ok("记录已提交独立审核",service.submit(datasetId,recordId,body.version(),body.reason()));}
    @PostMapping("/review") @PreAuthorize("hasAuthority('dataset:audit')") public ApiResponse<Map<String,Object>> review(@PathVariable long datasetId,@PathVariable String recordId,@Valid @RequestBody ReviewBody body,HttpServletRequest request){return ApiResponse.ok("审核完成并生成电子签名",service.review(datasetId,recordId,body.version(),body.approved(),body.reason(),body.secondaryPassword(),clientIps.resolve(request)));}
    @PostMapping("/publish") @PreAuthorize("hasAuthority('dataset:publish')") public ApiResponse<Map<String,Object>> publish(@PathVariable long datasetId,@PathVariable String recordId,@Valid @RequestBody SignBody body,HttpServletRequest request){return ApiResponse.ok("记录已发布并生成电子签名",service.publish(datasetId,recordId,body.version(),body.reason(),body.secondaryPassword(),clientIps.resolve(request)));}
    public record SubmitBody(@Positive int version,@NotBlank String reason){}
    public record ReviewBody(@Positive int version,boolean approved,@NotBlank String reason,@NotBlank @jakarta.validation.constraints.Pattern(regexp="^\\d{6}$") String secondaryPassword){}
    public record SignBody(@Positive int version,@NotBlank String reason,@NotBlank @jakarta.validation.constraints.Pattern(regexp="^\\d{6}$") String secondaryPassword){}
}
