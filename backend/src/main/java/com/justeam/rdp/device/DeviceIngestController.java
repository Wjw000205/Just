package com.justeam.rdp.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 无人员会话的设备专用采集入口，使用设备独立 HMAC 密钥验证。 */
@RestController
@RequestMapping("/api/device-ingest")
public class DeviceIngestController {
    private static final DefaultRedisScript<Long> RATE_SCRIPT=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n",Long.class);
    private final DeviceService devices;private final ObjectMapper mapper;private final AuditService audit;private final StringRedisTemplate redis;private final ClientIpResolver clientIps;
    public DeviceIngestController(DeviceService devices,ObjectMapper mapper,AuditService audit,StringRedisTemplate redis,ClientIpResolver clientIps){this.devices=devices;this.mapper=mapper;this.audit=audit;this.redis=redis;this.clientIps=clientIps;}
    @PostMapping("/{deviceCode}/measurements")
    public ApiResponse<Map<String,Long>> ingest(@PathVariable String deviceCode,
            @RequestHeader(value="X-Idempotency-Key",required=false) String sourceEventHeader,
            @RequestHeader(value="X-Timestamp",required=false) String timestampHeader,
            @RequestHeader(value="X-Signature",required=false) String signature,
            @RequestBody String rawBody,HttpServletRequest request){
        String ip=clientIps.resolve(request);if(!allow(ip,deviceCode)){auditRateLimited(ip,deviceCode);throw new BusinessException(429,"设备采集请求过于频繁");}
        try{if(sourceEventHeader==null||timestampHeader==null||signature==null||!signature.matches("(?i)^[0-9a-f]{64}$"))throw new BusinessException(401,"设备身份验证失败");UUID sourceEventId;long timestamp;try{sourceEventId=UUID.fromString(sourceEventHeader);timestamp=Long.parseLong(timestampHeader);}catch(Exception invalidHeader){throw new BusinessException(401,"设备身份验证失败");}DeviceService.VerifiedDevice verified=devices.verifyDeviceRequest(deviceCode,sourceEventId,timestamp,signature,rawBody);DeviceController.MeasurementBody body=mapper.readValue(rawBody,DeviceController.MeasurementBody.class);
            return ApiResponse.ok(Map.of("id",devices.ingestVerifiedDevice(verified,sourceEventId,body.toService())));
        }catch(BusinessException ex){if(ex.code()==401)auditFailure(ip,deviceCode,ex.getMessage());throw ex;}catch(Exception ex){throw BusinessException.badRequest("设备测点报文格式不正确");}
    }
    private boolean allow(String ip,String deviceCode){try{String minute=Long.toString(System.currentTimeMillis()/60000);long ipCount=increment("device-ingest:rate:ip:"+digest(ip)+":"+minute),deviceCount=increment("device-ingest:rate:device:"+digest(String.valueOf(deviceCode))+":"+minute);return ipCount<=120&&deviceCount<=30;}catch(Exception ignored){return true;}}
    private long increment(String key){Long value=redis.execute(RATE_SCRIPT,List.of(key),"120");return value==null?Long.MAX_VALUE:value;}
    private void auditRateLimited(String ip,String code){try{String marker="device-ingest:rate:audit:"+Integer.toHexString(ip.hashCode())+":"+(System.currentTimeMillis()/60000);if(Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(marker,"1",Duration.ofMinutes(2))))audit.recordAs(null,"device:rate-limited","RATE_LIMITED","DEVICE","设备采集请求被限流",Map.of("clientIp",ip,"deviceCodeDigest",Integer.toHexString(String.valueOf(code).hashCode())));}catch(Exception ignored){}}
    private void auditFailure(String ip,String deviceCode,String reason){String code=deviceCode==null?"unknown":deviceCode.substring(0,Math.min(deviceCode.length(),100));audit.recordAs(null,"device:"+code,"AUTH_FAILED","DEVICE","设备采集身份验证失败",Map.of("deviceCode",code,"clientIp",ip,"reason",reason));}
    private String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)),0,8);}catch(Exception ex){return Integer.toHexString(value.hashCode());}}
}
