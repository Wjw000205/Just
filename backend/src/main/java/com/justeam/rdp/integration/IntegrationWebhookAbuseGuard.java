package com.justeam.rdp.integration;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Redis fail-closed fixed-window guard for the anonymous HMAC endpoint. */
@Component
public class IntegrationWebhookAbuseGuard {
    private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>("""
            local ip=tonumber(redis.call('GET',KEYS[1]) or '0')
            local system=tonumber(redis.call('GET',KEYS[2]) or '0')
            local global=tonumber(redis.call('GET',KEYS[3]) or '0')
            if ip>=tonumber(ARGV[2]) then return -1 end
            if system>=tonumber(ARGV[3]) then return -2 end
            if global>=tonumber(ARGV[4]) then return -3 end
            ip=redis.call('INCR',KEYS[1]);if ip==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
            system=redis.call('INCR',KEYS[2]);if system==1 then redis.call('EXPIRE',KEYS[2],ARGV[1]) end
            global=redis.call('INCR',KEYS[3]);if global==1 then redis.call('EXPIRE',KEYS[3],ARGV[1]) end
            return 1
            """,Long.class);
    private final StringRedisTemplate redis;private final AuditService audit;private final int window,ipLimit,systemLimit,globalLimit;
    private final ConcurrentHashMap<String,Long> localAuditWindows=new ConcurrentHashMap<>();
    private final AtomicLong nextLocalCleanup=new AtomicLong();
    public IntegrationWebhookAbuseGuard(StringRedisTemplate redis,AuditService audit,
            @Value("${rdp.integration.rate-window-seconds:60}")int window,
            @Value("${rdp.integration.ip-rate-limit:120}")int ipLimit,
            @Value("${rdp.integration.system-rate-limit:300}")int systemLimit,
            @Value("${rdp.integration.global-rate-limit:1000}")int globalLimit){this.redis=redis;this.audit=audit;this.window=Math.max(10,window);this.ipLimit=Math.max(1,ipLimit);this.systemLimit=Math.max(1,systemLimit);this.globalLimit=Math.max(1,globalLimit);}
    public void check(String systemCode,String clientIp){String clientDigest=sha256(clientIp==null?"unknown":clientIp.trim()),systemDigest=sha256(systemCode==null?"unknown":systemCode.trim().toUpperCase());try{Long result=redis.execute(SCRIPT,List.of("integration:rate:ip:"+clientDigest,"integration:rate:system:"+systemDigest,"integration:rate:global"),Integer.toString(window),Integer.toString(ipLimit),Integer.toString(systemLimit),Integer.toString(globalLimit));if(result==null)throw new IllegalStateException("rate result missing");if(result<0){String scope=result==-1?"IP":result==-2?"SYSTEM":"GLOBAL";auditRateLimitOnce(scope,clientDigest,systemDigest);throw new BusinessException(429,"Webhook请求过于频繁，请稍后重试");}}catch(BusinessException ex){throw ex;}catch(Exception ex){auditRedisFailureOnce(clientDigest,systemDigest);throw new BusinessException(503,"集成安全服务暂不可用，请稍后重试");}}
    private void auditRateLimitOnce(String scope,String clientDigest,String systemDigest){String discriminator=switch(scope){case "IP"->clientDigest;case "SYSTEM"->systemDigest;default->"all";};String marker="integration:rate:audit:"+scope.toLowerCase()+":"+discriminator;boolean first;try{first=Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(marker,"1",Duration.ofSeconds(window)));}catch(Exception ex){first=localFirst(marker);}if(first)safeAudit("INTEGRATION_RATE_LIMITED","外部集成入口超过频率限制",Map.of("scope",scope,"clientDigest",clientDigest,"systemDigest",systemDigest));}
    public void bodyTooLarge(String systemCode,String clientIp){String clientDigest=sha256(clientIp==null?"unknown":clientIp.trim()),systemDigest=sha256(systemCode==null?"unknown":systemCode.trim().toUpperCase());String marker="body:"+clientDigest;if(localFirst(marker))safeAudit("INTEGRATION_BODY_REJECTED","外部集成请求体超过1MB读取上限",Map.of("clientDigest",clientDigest,"systemDigest",systemDigest,"maxBytes",1_048_576));}
    private void auditRedisFailureOnce(String clientDigest,String systemDigest){if(localFirst("redis:global"))safeAudit("INTEGRATION_RATE_SERVICE_FAILED","外部集成限流因Redis不可用而失败关闭",Map.of("clientDigest",clientDigest,"systemDigest",systemDigest));}
    private boolean localFirst(String key){long now=System.currentTimeMillis(),until=now+window*1000L,next=nextLocalCleanup.get();if(now>=next&&nextLocalCleanup.compareAndSet(next,until))localAuditWindows.entrySet().removeIf(entry->entry.getValue()<now);if(localAuditWindows.size()>=2048&&!localAuditWindows.containsKey(key))return false;return localAuditWindows.putIfAbsent(key,until)==null;}
    private void safeAudit(String operation,String description,Map<String,Object> details){try{audit.recordIndependent(null,"anonymous",operation,"INTEGRATION",description,details);}catch(Exception ignored){/* Security response must not become an audit-write amplification path. */}}
    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
