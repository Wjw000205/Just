package com.justeam.rdp.auth;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.security.TokenService;
import com.justeam.rdp.security.UserAccountService;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private static final DefaultRedisScript<Long> FAILURE_SCRIPT=new DefaultRedisScript<>("""
            local count=redis.call('INCR',KEYS[1])
            if count==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
            if count>=tonumber(ARGV[2]) then redis.call('SET',KEYS[2],'1','EX',ARGV[1]) end
            return count
            """,Long.class);
    private final UserAccountService users;
    private final TokenService tokens;
    private final CaptchaService captchas;
    private final PasswordEncoder passwords;
    private final StringRedisTemplate redis;
    private final JdbcClient jdbc;
    private final AuditService audit;
    private final AuthAbuseGuard abuseGuard;

    public AuthService(UserAccountService users, TokenService tokens, CaptchaService captchas,
                       PasswordEncoder passwords, StringRedisTemplate redis, JdbcClient jdbc, AuditService audit,
                       AuthAbuseGuard abuseGuard) {
        this.users = users;
        this.tokens = tokens;
        this.captchas = captchas;
        this.passwords = passwords;
        this.redis = redis;
        this.jdbc = jdbc;
        this.audit = audit;
        this.abuseGuard = abuseGuard;
    }

    @Transactional
    public LoginResult login(LoginRequest request, String ip) {
        abuseGuard.login(ip);
        captchas.verify(request.captchaKey(), request.captchaCode());
        String accountHash=accountHash(request.username()),lockKey="login:lock:"+accountHash;
        try{if(Boolean.TRUE.equals(redis.hasKey(lockKey)))throw BusinessException.unauthorized("账号已临时锁定，请30分钟后重试");}
        catch(BusinessException ex){throw ex;}catch(Exception ex){redisUnavailable("CHECK_LOCK",accountHash);}
        UserPrincipal user;
        try {
            user = users.loadUserByUsername(request.username());
        } catch (BusinessException ex) {
            registerFailure(request.username(),accountHash);
            throw ex;
        }
        if (!passwords.matches(request.password(), user.password())) {
            registerFailure(request.username(),accountHash);
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (!user.enabled()) {
            String registrationStatus=jdbc.sql("SELECT status FROM sys_registration_application WHERE user_id=:user")
                    .param("user",user.id()).query(String.class).optional().orElse(null);
            if("PENDING".equals(registrationStatus))throw BusinessException.forbidden("注册申请正在等待管理员审核");
            if("REJECTED".equals(registrationStatus))throw BusinessException.forbidden("注册申请已被驳回，请联系管理员");
            throw BusinessException.forbidden("账号已停用，请联系管理员");
        }
        try{redis.delete(List.of("login:fail:"+accountHash,lockKey));}catch(Exception ex){redisUnavailable("CLEAR_FAILURE",accountHash);}
        TokenService.TokenPair pair = tokens.issue(user);
        jdbc.sql("UPDATE sys_user SET last_login_time = now(), last_login_ip = :ip, failed_attempts = 0 WHERE id = :id")
                .param("ip", ip).param("id", user.id()).update();
        audit.recordAs(user.id(), user.username(), "LOGIN", "AUTH", "用户登录成功", Map.of("ip", ip));
        return new LoginResult(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresIn(), userView(user));
    }

    public LoginResult refresh(String refreshToken) {
        long userId = tokens.consumeRefresh(refreshToken);
        UserPrincipal user = users.loadById(userId);
        if (!user.enabled()) throw BusinessException.forbidden("账号已停用");
        TokenService.TokenPair pair = tokens.issue(user);
        return new LoginResult(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresIn(), userView(user));
    }

    public void logout(long userId) {
        tokens.revoke(userId);
        audit.record("LOGOUT", "AUTH", "用户退出登录");
    }

    private void registerFailure(String username,String accountHash) {
        try{Long count=redis.execute(FAILURE_SCRIPT,List.of("login:fail:"+accountHash,"login:lock:"+accountHash),"1800","5");if(count==null)throw new IllegalStateException("Redis login failure script returned null");}
        catch(Exception ex){redisUnavailable("REGISTER_FAILURE",accountHash);}
        jdbc.sql("UPDATE sys_user SET failed_attempts = failed_attempts + 1 WHERE username = :username")
                .param("username", username).update();
        audit.recordIndependent(null,"anonymous","LOGIN_FAILED","AUTH","用户名或密码校验失败",Map.of("accountDigest",accountHash));
    }

    private String accountHash(String username){String normalized=username==null?"":username.trim().toLowerCase(java.util.Locale.ROOT);try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private void redisUnavailable(String operation,String accountHash){audit.recordIndependent(null,"anonymous","LOGIN_SERVICE_FAILED","AUTH","登录因Redis安全状态不可用被拒绝",Map.of("operation",operation,"accountDigest",accountHash));throw new BusinessException(503,"登录安全服务暂不可用，请稍后重试");}

    private Map<String, Object> userView(UserPrincipal user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.id());
        result.put("username", user.username());
        result.put("realName", user.realName());
        result.put("roles", user.roles());
        result.put("permissions", user.permissions());
        result.put("assignedScopes", user.assignedScopes());
        result.put("dataScopes", user.dataScopes());
        result.put("mustChangePassword", user.mustChangePassword());
        return result;
    }

    public record LoginRequest(String username, String password, String captchaKey, String captchaCode) {}
    public record LoginResult(String accessToken, @com.fasterxml.jackson.annotation.JsonIgnore String refreshToken, String tokenType, long expiresIn,
                              Map<String, Object> userInfo) {}
}
