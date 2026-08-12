package com.justeam.rdp.security;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Shared brute-force protection for every use of the six-digit secondary credential. */
@Service
public class SecondaryCredentialService {
    private static final int MAX_FAILURES=5;
    private static final long LOCK_SECONDS=30*60;
    private static final DefaultRedisScript<Long> FAILURE_SCRIPT=new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; " +
                    "if n>=tonumber(ARGV[2]) then redis.call('SET',KEYS[2],'1','EX',ARGV[3]); end; return n",
            Long.class);

    private final StringRedisTemplate redis;
    private final PasswordEncoder passwords;
    private final AuditService audit;

    public SecondaryCredentialService(StringRedisTemplate redis,PasswordEncoder passwords,AuditService audit){
        this.redis=redis;this.passwords=passwords;this.audit=audit;
    }

    public void verify(UserPrincipal user,String candidate,String hash,String purpose){
        if(candidate==null||!candidate.matches("^\\d{6}$"))throw BusinessException.badRequest("请输入6位二级密码");
        if(hash==null||hash.isBlank())throw BusinessException.badRequest("请先在个人中心设置二级密码");
        boolean signature="SIGNATURE".equals(purpose);
        verifyProtected(user,()->passwords.matches(candidate,hash),failureKey(user.id()),lockKey(user.id()),purpose,
                signature?"SIGNATURE_AUTH_FAILED":"SECONDARY_AUTH_FAILED",signature?"DATASET":"USER","二级密码校验失败");
    }

    public void verifyLoginPasswordForSetup(UserPrincipal user,String candidate,String hash){
        if(hash==null||hash.isBlank())throw BusinessException.unauthorized("账号不可用");
        verifyProtected(user,()->candidate!=null&&!candidate.isBlank()&&passwords.matches(candidate,hash),
                setupFailureKey(user.id()),setupLockKey(user.id()),"SET_SECONDARY_PASSWORD",
                "SECONDARY_SETUP_AUTH_FAILED","USER","当前登录密码复核失败");
    }

    private void verifyProtected(UserPrincipal user,BooleanSupplier matcher,String failureKey,String lockKey,
                                 String purpose,String operation,String module,String failureMessage){
        try{if(Boolean.TRUE.equals(redis.hasKey(lockKey)))throw new BusinessException(429,"二级密码校验失败次数过多，请30分钟后重试");}
        catch(BusinessException ex){throw ex;}catch(Exception ex){throw unavailable();}
        if(matcher.getAsBoolean()){
            try{redis.delete(List.of(failureKey,lockKey));}catch(Exception ex){throw unavailable();}
            return;
        }
        long attempts;
        try{
            Long value=redis.execute(FAILURE_SCRIPT,List.of(failureKey,lockKey),Long.toString(LOCK_SECONDS),Integer.toString(MAX_FAILURES),Long.toString(LOCK_SECONDS));
            attempts=value==null?MAX_FAILURES:value;
        }catch(Exception ex){throw unavailable();}
        boolean locked=attempts>=MAX_FAILURES;
        audit.recordIndependent(user.id(),user.username(),operation,module,failureMessage,
                Map.of("userId",user.id(),"purpose",purpose,"attempt",attempts,"locked",locked));
        if(locked)throw new BusinessException(429,"二级密码校验失败次数过多，请30分钟后重试");
        throw BusinessException.unauthorized(failureMessage);
    }

    public String failureKey(long userId){return "security:secondary:fail:"+userId;}
    public String lockKey(long userId){return "security:secondary:lock:"+userId;}
    public String setupFailureKey(long userId){return "security:secondary-setup:fail:"+userId;}
    public String setupLockKey(long userId){return "security:secondary-setup:lock:"+userId;}
    private BusinessException unavailable(){return new BusinessException(503,"二级认证保护服务暂不可用");}
}
