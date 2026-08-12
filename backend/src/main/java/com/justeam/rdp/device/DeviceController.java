package com.justeam.rdp.device;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService service;public DeviceController(DeviceService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('device:read')")
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam(required=false)String status,@RequestParam(required=false)String keyword){return ApiResponse.ok(service.list(status,keyword));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('device:read')")
    public ApiResponse<Map<String,Object>> get(@PathVariable long id){return ApiResponse.ok(service.get(id));}
    @PostMapping @PreAuthorize("hasAuthority('device:manage')")
    public ApiResponse<Map<String,Object>> create(@Valid @RequestBody DeviceBody b){DeviceService.DeviceRegistration registration=service.create(b.toService());return ApiResponse.ok("设备已登记；采集密钥仅返回本次，请立即安全保存",Map.of("id",registration.id(),"deviceSecret",registration.deviceSecret()));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('device:manage')")
    public ApiResponse<Void> update(@PathVariable long id,@Valid @RequestBody DeviceBody b){service.update(id,b.toService());return ApiResponse.ok("设备已更新",null);}
    @PostMapping("/{id}/measurements") @PreAuthorize("hasAuthority('device:manage')")
    public ApiResponse<Map<String,Long>> ingest(@PathVariable long id,@RequestHeader("X-Idempotency-Key") java.util.UUID sourceEventId,
       @Valid @RequestBody MeasurementBody b){return ApiResponse.ok(Map.of("id",service.ingest(id,sourceEventId,b.toService())));}
    @GetMapping("/{id}/measurements") @PreAuthorize("hasAuthority('device:read')")
    public ApiResponse<PageResponse<Map<String,Object>>> measurements(@PathVariable long id,@RequestParam(required=false)String metric,
      @RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="100")int pageSize){return ApiResponse.ok(service.measurements(id,metric,from,to,pageNum,pageSize));}
    @PostMapping("/{id}/rotate-secret") @PreAuthorize("hasAuthority('device:manage')")
    public ApiResponse<Map<String,Object>> rotateSecret(@PathVariable long id){DeviceService.DeviceRegistration registration=service.rotateIngestSecret(id);return ApiResponse.ok("采集密钥已轮换；旧密钥立即失效",Map.of("id",registration.id(),"deviceSecret",registration.deviceSecret()));}
    @PutMapping("/{id}/polling") @PreAuthorize("hasAuthority('device:manage')")
    public ApiResponse<Void> polling(@PathVariable long id,@Valid @RequestBody PollingBody body){service.configurePolling(id,body.enabled(),body.intervalSeconds(),body.heartbeatTimeoutSeconds());return ApiResponse.ok(body.enabled()?"自动采集已启用":"自动采集已停用",null);}
    public record DeviceBody(@NotBlank String deviceCode,@NotBlank String deviceName,@NotBlank String deviceType,String model,
       @NotBlank String protocol,@NotNull Map<String,Object> connectionConfig,@Positive long dataScopeId){DeviceService.DeviceBody toService(){return new DeviceService.DeviceBody(deviceCode,deviceName,deviceType,model,protocol,connectionConfig,dataScopeId);}}
    public record MeasurementBody(@NotBlank String metricName,java.math.BigDecimal metricValue,String textValue,String unit,
       @Pattern(regexp="GOOD|UNCERTAIN|BAD")String quality,Instant measuredTime,@NotNull Map<String,Object> rawData){DeviceService.MeasurementBody toService(){return new DeviceService.MeasurementBody(metricName,metricValue,textValue,unit,quality,measuredTime,rawData);}}
    public record PollingBody(boolean enabled,@Min(1) @Max(86400) int intervalSeconds,@Min(10) @Max(86400) int heartbeatTimeoutSeconds){}
}
