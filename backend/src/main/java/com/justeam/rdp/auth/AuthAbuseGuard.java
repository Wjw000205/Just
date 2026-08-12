package com.justeam.rdp.auth;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.security.RdpProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuthAbuseGuard {
    private static final DefaultRedisScript<Long> RATE_SCRIPT=new DefaultRedisScript<>("""
            local ipCount=tonumber(redis.call('GET',KEYS[1]) or '0')
            if ipCount>=tonumber(ARGV[2]) then return -1 end
            local globalCount=tonumber(redis.call('GET',KEYS[2]) or '0')
            if globalCount>=tonumber(ARGV[3]) then return -2 end
            ipCount=redis.call('INCR',KEYS[1])
            if ipCount==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
            globalCount=redis.call('INCR',KEYS[2])
            if globalCount==1 then redis.call('EXPIRE',KEYS[2],ARGV[1]) end
            return 1
            """,Long.class);
    private final StringRedisTemplate redis;
    private final RdpProperties properties;
    private final AuditService audit;

    public AuthAbuseGuard(StringRedisTemplate redis,RdpProperties properties,AuditService audit){
        this.redis=redis;this.properties=properties;this.audit=audit;
    }

    public void captcha(String ip){limit("CAPTCHA",ip,properties.security().captchaIpRateLimit(),properties.security().captchaGlobalRateLimit());}
    public void login(String ip){limit("LOGIN",ip,properties.security().loginIpRateLimit(),properties.security().loginGlobalRateLimit());}

    private void limit(String operation,String ip,int ipLimit,int globalLimit){
        long window=Math.max(60,properties.security().authRateWindowSeconds());
        String digest=sha256(ip==null?"unknown":ip.trim());
        String prefix="auth:rate:"+operation.toLowerCase();
        try{
            Long result=redis.execute(RATE_SCRIPT,List.of(prefix+":ip:"+digest,prefix+":global"),Long.toString(window),Integer.toString(Math.max(1,ipLimit)),Integer.toString(Math.max(1,globalLimit)));
            if(result==null)throw new IllegalStateException("Redis auth rate script returned null");
            if(result<0){String scope=result==-1?"IP":"GLOBAL";audit.recordIndependent(null,"anonymous","AUTH_RATE_LIMITED","AUTH","匿名认证请求超过频率限制",Map.of("operation",operation,"scope",scope));throw new BusinessException(429,"请求过于频繁，请稍后重试");}
        }catch(BusinessException ex){throw ex;}catch(Exception ex){
            audit.recordIndependent(null,"anonymous","AUTH_RATE_SERVICE_FAILED","AUTH","匿名认证限流因Redis不可用而失败关闭",Map.of("operation",operation,"clientDigest",digest));
            throw new BusinessException(503,"认证安全服务暂不可用，请稍后重试");
        }
    }

    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
