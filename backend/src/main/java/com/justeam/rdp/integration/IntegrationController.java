package com.justeam.rdp.integration;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.ClientIpResolver;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
    private final IntegrationService service;private final ClientIpResolver clientIps;private final StepUpService stepUp;public IntegrationController(IntegrationService service,ClientIpResolver clientIps,StepUpService stepUp){this.service=service;this.clientIps=clientIps;this.stepUp=stepUp;}
    @GetMapping @PreAuthorize("hasAnyAuthority('integration:read','integration:manage')") public ApiResponse<List<Map<String,Object>>> configs(){var user=CurrentUser.require();return ApiResponse.ok(service.configs(user.permissions().contains("integration:manage")));}
    @PostMapping @PreAuthorize("hasAuthority('integration:manage')") public ApiResponse<Map<String,Long>> create(@Valid @RequestBody ConfigBody body){return ApiResponse.ok("集成配置已创建",Map.of("id",service.create(body.toService())));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('integration:manage')") public ApiResponse<Void> update(@PathVariable long id,@Valid @RequestBody ConfigBody body){service.update(id,body.toService());return ApiResponse.ok("集成配置已更新",null);}
    @GetMapping("/{id}/mappings") @PreAuthorize("hasAnyAuthority('integration:read','integration:manage')") public ApiResponse<List<Map<String,Object>>> mappings(@PathVariable long id){return ApiResponse.ok(service.mappings(id));}
    @PutMapping("/{id}/mappings") @PreAuthorize("hasAuthority('integration:manage')") public ApiResponse<Void> mappings(@PathVariable long id,@RequestParam int expectedMappingVersion,@RequestBody List<@Valid MappingBody> body){service.replaceMappings(id,expectedMappingVersion,body.stream().map(MappingBody::toService).toList());return ApiResponse.ok("字段映射已保存",null);}
    @GetMapping("/jobs") @PreAuthorize("hasAnyAuthority('integration:read','integration:manage')") public ApiResponse<List<Map<String,Object>>> jobs(@RequestParam(required=false)String systemCode,@RequestParam(required=false)String status){return ApiResponse.ok(service.jobs(systemCode,status));}
    @GetMapping("/dead-letters") @PreAuthorize("hasAnyAuthority('integration:read','integration:manage')") public ApiResponse<List<Map<String,Object>>> deadLetters(@RequestParam(required=false)String status){return ApiResponse.ok(service.deadLetters(status));}
    @PostMapping("/dead-letters/{id}/retry") @PreAuthorize("hasAuthority('integration:retry')") public ResponseEntity<ApiResponse<Map<String,Object>>> retry(@PathVariable long id,@Valid @RequestBody RetryBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);return ResponseEntity.accepted().body(ApiResponse.ok("死信已进入后台重放队列",service.retry(id,body.note(),body.useCurrentMapping())));}
    @PostMapping("/{id}/reconcile") @PreAuthorize("hasAuthority('integration:manage')") public ApiResponse<Map<String,Object>> reconcile(@PathVariable long id){return ApiResponse.ok("集成落地对账完成",service.reconcile(id));}
    @PostMapping("/webhook/{systemCode}") public ResponseEntity<ApiResponse<Map<String,Object>>> webhook(@PathVariable String systemCode,@RequestHeader(value="X-Timestamp",required=false)String timestamp,@RequestHeader(value="X-Signature",required=false)String signature,@RequestHeader(value="X-Idempotency-Key",required=false)String key,@RequestHeader(value=HttpHeaders.AUTHORIZATION,required=false)String authorization,@RequestBody String body,HttpServletRequest request){String clientIp=clientIps.resolve(request);IntegrationService.WebhookResult result=service.acceptWebhook(systemCode,timestamp,signature,key,authorization,body,clientIp);request.setAttribute("rdp.integration.authType",result.authType());request.setAttribute("rdp.integration.jobId",result.jobId());return ResponseEntity.accepted().body(ApiResponse.ok("事件已认证并持久接收，后台处理状态可按作业编号查询",Map.of("jobId",result.jobId(),"status",result.status(),"created",result.created())));}
    public record ConfigBody(@NotBlank String systemCode,@NotBlank String systemName,@NotBlank String systemType,String baseUrl,@NotBlank String authType,String secret,boolean active,@NotNull Map<String,Object> config,Integer expectedVersion){IntegrationService.ConfigBody toService(){return new IntegrationService.ConfigBody(systemCode,systemName,systemType,baseUrl,authType,secret,active,config,expectedVersion);}}
    public record MappingBody(@NotBlank String sourceField,@NotBlank String targetField,@NotBlank String transformation,boolean required,String defaultValue){IntegrationService.MappingBody toService(){return new IntegrationService.MappingBody(sourceField,targetField,transformation,required,defaultValue);}}
    public record RetryBody(@NotBlank @Size(max=1000)String note,boolean useCurrentMapping){}
}
