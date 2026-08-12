package com.justeam.rdp.auth;

import com.justeam.rdp.admin.AdminService;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.RdpProperties;
import com.justeam.rdp.security.TokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Array;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {
    private static final DefaultRedisScript<Long> RATE_SCRIPT = new DefaultRedisScript<>("""
            local count=redis.call('INCR',KEYS[1])
            if count==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
            if count>tonumber(ARGV[2]) then return 0 end
            return count
            """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local current=redis.call('GET',KEYS[1])
            if not current then return 0 end
            if current==ARGV[1] then
              redis.call('DEL',KEYS[1]);redis.call('DEL',KEYS[2]);return 1
            end
            local attempts=redis.call('INCR',KEYS[2])
            if attempts==1 then redis.call('EXPIRE',KEYS[2],ARGV[2]) end
            if attempts>=tonumber(ARGV[3]) then redis.call('DEL',KEYS[1]) end
            return -attempts
            """, Long.class);

    private final JdbcClient jdbc;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwords;
    private final CaptchaService captchas;
    private final AuditService audit;
    private final RdpProperties properties;
    private final RegistrationNotificationGateway notifications;
    private final AdminService admin;
    private final TokenService tokens;
    private final SecureRandom random = new SecureRandom();

    public RegistrationService(JdbcClient jdbc, StringRedisTemplate redis, PasswordEncoder passwords,
                               CaptchaService captchas, AuditService audit, RdpProperties properties,
                               RegistrationNotificationGateway notifications, AdminService admin,
                               TokenService tokens) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.passwords = passwords;
        this.captchas = captchas;
        this.audit = audit;
        this.properties = properties;
        this.notifications = notifications;
        this.admin = admin;
        this.tokens = tokens;
    }

    public Map<String,Object> settings() {
        RdpProperties.Registration config = config();
        return Map.of("enabled", config.enabled(), "channels", channels(),
                "codeTtlSeconds", boundedTtl(), "resendSeconds", boundedResend(),
                "notificationMode", notifications.adapterCode());
    }

    public CodeDispatch requestCode(CodeRequest request, String clientIp) {
        requireEnabled();
        captchas.verify(request.captchaKey(), request.captchaCode());
        String channel = normalizeChannel(request.channel());
        String target = normalizeTarget(channel, request.target());
        String targetHash = sha256(channel + "|" + target);
        String ipHash = sha256(clientIp == null ? "unknown" : clientIp);
        enforceRate("registration:rate:global", config().globalRateLimit());
        enforceRate("registration:rate:target:" + targetHash, config().targetRateLimit());
        enforceRate("registration:rate:ip:" + ipHash, config().ipRateLimit());
        String cooldownKey = "registration:cooldown:" + targetHash;
        try {
            Boolean allowed = redis.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(boundedResend()));
            if (!Boolean.TRUE.equals(allowed)) throw new BusinessException(429, "验证码发送过于频繁，请稍后重试");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_FAILED", "AUTH",
                    "注册验证码发送因Redis不可用被拒绝", Map.of("channel", channel, "target", mask(target)));
            throw new BusinessException(503, "验证码服务暂不可用，请稍后重试");
        }
        String verificationId = UUID.randomUUID().toString();
        String code = "%06d".formatted(random.nextInt(1_000_000));
        String codeKey = codeKey(verificationId);
        try {
            redis.opsForValue().set(codeKey, otpDigest(channel, target, code), Duration.ofSeconds(boundedTtl()));
            notifications.deliver(new RegistrationNotificationGateway.Delivery(null, channel, target,
                    "REGISTRATION_OTP", "REGISTRATION_VERIFICATION", verificationId,
                    Map.of("purpose", "REGISTER", "expiresInSeconds", boundedTtl(),
                            "channel", channel, "target", mask(target)), code));
        } catch (Exception ex) {
            try { redis.delete(List.of(codeKey, cooldownKey)); } catch (Exception ignored) {}
            audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_FAILED", "AUTH",
                    "注册验证码模拟投递失败", Map.of("channel", channel, "target", mask(target)));
            if (ex instanceof BusinessException business) throw business;
            throw new BusinessException(503, "通知服务暂不可用，请稍后重试");
        }
        audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_SENT", "AUTH",
                "注册验证码已发送", Map.of("verificationId", verificationId, "channel", channel,
                        "target", mask(target), "clientIp", clientIp == null ? "unknown" : clientIp));
        return new CodeDispatch(verificationId, boundedTtl(), boundedResend(), mask(target));
    }

    @Transactional
    public long register(RegisterRequest request) {
        requireEnabled();
        captchas.verify(request.captchaKey(), request.captchaCode());
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String channel = normalizeChannel(request.verificationChannel());
        String target = normalizeTarget(channel, "EMAIL".equals(channel) ? request.email() : request.phone());
        // Only the contact proven by this one-time code is persisted and can reserve a unique value.
        // The other channel can be bound later through its own verified flow instead of trusting form text.
        String email = "EMAIL".equals(channel) ? target : null;
        String phone = "PHONE".equals(channel) ? target : null;
        if (!request.password().equals(request.confirmPassword())) throw BusinessException.badRequest("两次输入的密码不一致");
        if (!request.password().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$")) {
            throw BusinessException.badRequest("密码需为8-20位并包含大小写字母和数字");
        }
        consumeCode(request.verificationId(), channel, target, request.verificationCode());
        long duplicates = jdbc.sql("""
                SELECT count(*) FROM sys_user WHERE deleted=0 AND
                    (lower(username)=:username
                     OR (CAST(:email AS text) IS NOT NULL AND lower(email)=CAST(:email AS text))
                     OR (CAST(:phone AS text) IS NOT NULL AND phone=CAST(:phone AS text)))
                """)
                .param("username", username).param("email", email).param("phone", phone).query(Long.class).single();
        if (duplicates > 0) throw new BusinessException(409, "用户名、邮箱或手机号已存在");
        Long userId = jdbc.sql("""
                INSERT INTO sys_user(username,real_name,email,phone,password,status)
                VALUES (:username,:realName,:email,:phone,:password,0) RETURNING id
                """).param("username", username).param("realName", request.realName().trim())
                .param("email", email).param("phone", phone).param("password", passwords.encode(request.password()))
                .query(Long.class).single();
        Long guestRole = jdbc.sql("SELECT id FROM sys_role WHERE role_code='GUEST' AND status=1 AND deleted=0")
                .query(Long.class).optional().orElseThrow(() -> new IllegalStateException("GUEST role is unavailable"));
        jdbc.sql("INSERT INTO sys_user_role(user_id,role_id) VALUES (:user,:role)")
                .param("user", userId).param("role", guestRole).update();
        Long applicationId = jdbc.sql("""
                INSERT INTO sys_registration_application(user_id,verification_channel,destination_masked,destination_digest)
                VALUES (:user,:channel,:destination,:digest) RETURNING id
                """).param("user", userId).param("channel", channel).param("destination", mask(target))
                .param("digest", destinationDigest(channel, target))
                .query(Long.class).single();
        audit.recordAs(userId, username, "REGISTER", "AUTH", "提交自助注册申请",
                Map.of("applicationId", applicationId, "status", "PENDING", "channel", channel,
                        "destination", mask(target)));
        notifySubmitted(userId, applicationId, username, channel, target);
        notifications.forgetVerificationCode(request.verificationId());
        return applicationId;
    }

    public PageResponse<Map<String,Object>> applications(String status, String keyword, int pageNum, int pageSize) {
        String normalizedStatus = blank(status) == null ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalizedStatus != null && !List.of("PENDING","APPROVED","REJECTED").contains(normalizedStatus)) {
            throw BusinessException.badRequest("注册审核状态不正确");
        }
        int page = Math.max(1, pageNum), size = Math.max(1, Math.min(200, pageSize));
        String where = " FROM sys_registration_application a JOIN sys_user u ON u.id=a.user_id LEFT JOIN sys_department d ON d.id=u.department_id WHERE u.deleted=0 AND (CAST(:status AS text) IS NULL OR a.status=CAST(:status AS text)) AND (CAST(:keyword AS text) IS NULL OR lower(u.username) LIKE lower(concat('%',CAST(:keyword AS text),'%')) OR lower(u.real_name) LIKE lower(concat('%',CAST(:keyword AS text),'%')))";
        long total = jdbc.sql("SELECT count(*)" + where).param("status", normalizedStatus).param("keyword", blank(keyword))
                .query(Long.class).single();
        List<Map<String,Object>> rows = jdbc.sql("""
                SELECT a.id,a.user_id,a.verification_channel,a.destination_masked,a.status,a.submitted_time,
                       a.reviewed_by,a.reviewed_time,a.review_comment,a.version,
                       u.username,u.real_name,u.department_id,d.dept_name,u.authorization_version,
                       ARRAY(SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=u.id ORDER BY r.sort_order,r.id) roles,
                       ARRAY(SELECT data_scope_id FROM sys_user_data_scope WHERE user_id=u.id ORDER BY data_scope_id) scopes
                """ + where + " ORDER BY a.submitted_time DESC,a.id DESC LIMIT :limit OFFSET :offset")
                .param("status", normalizedStatus).param("keyword", blank(keyword)).param("limit", size)
                .param("offset", (page - 1) * size).query((rs,n) -> {
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id")); row.put("userId", rs.getLong("user_id"));
                    row.put("username", rs.getString("username")); row.put("realName", rs.getString("real_name"));
                    row.put("verificationChannel", rs.getString("verification_channel"));
                    row.put("destinationMasked", rs.getString("destination_masked")); row.put("status", rs.getString("status"));
                    row.put("submittedTime", rs.getObject("submitted_time")); row.put("reviewedTime", rs.getObject("reviewed_time"));
                    row.put("reviewComment", rs.getString("review_comment")); row.put("applicationVersion", rs.getInt("version"));
                    row.put("authorizationVersion", rs.getInt("authorization_version"));
                    row.put("departmentId", rs.getObject("department_id")); row.put("department", rs.getString("dept_name"));
                    row.put("roles", array(rs.getArray("roles"))); row.put("dataScopes", array(rs.getArray("scopes")));
                    return row;
                }).list();
        return PageResponse.of(total, page, size, rows);
    }

    @Transactional
    public void review(long applicationId, ReviewRequest request) {
        String decision = request.decision() == null ? "" : request.decision().trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVE","REJECT").contains(decision)) throw BusinessException.badRequest("审核决定只支持APPROVE或REJECT");
        // Keep the global governance lock order identical to all other user mutations.
        adminMutationLock();
        Map<String,Object> application = jdbc.sql("""
                SELECT a.user_id,a.status,a.version,u.username,u.status AS user_status,
                       a.verification_channel,a.destination_digest,u.email,u.phone
                FROM sys_registration_application a JOIN sys_user u ON u.id=a.user_id AND u.deleted=0
                WHERE a.id=:id FOR UPDATE OF a,u
                """).param("id", applicationId).query((rs,n) -> {
                    Map<String,Object> row = new LinkedHashMap<>();
                    String channel = rs.getString("verification_channel");
                    row.put("userId", rs.getLong("user_id")); row.put("status", rs.getString("status"));
                    row.put("version", rs.getInt("version")); row.put("username", rs.getString("username"));
                    row.put("userStatus", rs.getInt("user_status")); row.put("channel", channel);
                    row.put("destinationDigest", rs.getString("destination_digest"));
                    row.put("target", "EMAIL".equals(channel) ? rs.getString("email") : rs.getString("phone"));
                    return row;
                })
                .optional().orElseThrow(() -> BusinessException.notFound("注册申请不存在"));
        if (!"PENDING".equals(application.get("status"))) throw new BusinessException(409, "该注册申请已完成审核");
        if (((Number)application.get("version")).intValue() != request.applicationVersion()) {
            throw new BusinessException(409, "注册申请版本已变化，请刷新后重试");
        }
        requireVerifiedDestination(application);
        long userId = ((Number)application.get("userId")).longValue();
        String afterStatus = "APPROVE".equals(decision) ? "APPROVED" : "REJECTED";
        int updated = jdbc.sql("""
                UPDATE sys_registration_application SET status=:status,reviewed_by=:actor,reviewed_time=now(),
                    review_comment=:comment,version=version+1 WHERE id=:id AND status='PENDING' AND version=:version
                """).param("status", afterStatus).param("actor", CurrentUser.require().id())
                .param("comment", request.comment().trim()).param("id", applicationId)
                .param("version", request.applicationVersion()).update();
        if (updated == 0) throw new BusinessException(409, "注册申请已被其他审核员处理");
        if ("APPROVE".equals(decision)) {
            if (request.roleIds() == null || request.roleIds().isEmpty()) throw BusinessException.badRequest("批准时至少分配一个角色");
            if (request.scopeIds() == null || request.scopeIds().isEmpty()) throw BusinessException.badRequest("批准时至少分配一个直接数据域");
            admin.assign(userId, request.roleIds(), request.scopeIds(), request.departmentId(),
                    "注册审批授权：" + request.comment().trim(), request.authorizationVersion());
            admin.setStatus(userId, 1, "注册申请批准：" + request.comment().trim());
        } else {
            tokens.revoke(userId);
        }
        audit.record("APPROVE".equals(decision) ? "REGISTRATION_APPROVE" : "REGISTRATION_REJECT",
                "AUTH", "审核自助注册申请", Map.of("applicationId", applicationId, "userId", userId,
                        "before", "PENDING", "after", afterStatus, "reason", request.comment().trim()));
        String username = String.valueOf(application.get("username"));
        String channel = String.valueOf(application.get("channel"));
        String target = String.valueOf(application.get("target"));
        notifications.deliver(new RegistrationNotificationGateway.Delivery(userId, channel, target,
                "REGISTRATION_" + afterStatus, "REGISTRATION_APPLICATION", Long.toString(applicationId),
                Map.of("username", username, "decision", afterStatus, "comment", request.comment().trim()), null));
    }

    public void requireApprovedBeforeEnable(long userId) {
        String status = jdbc.sql("SELECT status FROM sys_registration_application WHERE user_id=:user")
                .param("user", userId).query(String.class).optional().orElse(null);
        if (status != null && !"APPROVED".equals(status)) {
            throw new BusinessException(409, "自助注册账号必须通过注册审核流程后才能启用");
        }
    }

    private void notifySubmitted(long userId, long applicationId, String username, String channel, String target) {
        notifications.deliver(new RegistrationNotificationGateway.Delivery(userId, channel, target,
                "REGISTRATION_SUBMITTED", "REGISTRATION_APPLICATION", Long.toString(applicationId),
                Map.of("username", username, "status", "PENDING"), null));
        List<Map<String,Object>> administrators = jdbc.sql("""
                SELECT DISTINCT u.id,u.username,u.email,u.phone FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                WHERE u.deleted=0 AND u.status=1 AND r.role_code='ADMIN' AND r.status=1 AND r.deleted=0
                """).query((rs,n) -> {
                    Map<String,Object> adminUser = new LinkedHashMap<>();
                    adminUser.put("id", rs.getLong("id")); adminUser.put("username", rs.getString("username"));
                    adminUser.put("email", rs.getString("email")); adminUser.put("phone", rs.getString("phone"));
                    return adminUser;
                }).list();
        for (Map<String,Object> administrator : administrators) {
            String email = (String)administrator.get("email"), phone = (String)administrator.get("phone");
            String adminChannel = email != null && !email.isBlank() ? "EMAIL" : phone != null && !phone.isBlank() ? "PHONE" : "SYSTEM";
            String recipient = "EMAIL".equals(adminChannel) ? email : "PHONE".equals(adminChannel) ? phone : String.valueOf(administrator.get("username"));
            notifications.deliver(new RegistrationNotificationGateway.Delivery(((Number)administrator.get("id")).longValue(),
                    adminChannel, recipient, "REGISTRATION_REVIEW_REQUIRED", "REGISTRATION_APPLICATION",
                    Long.toString(applicationId), Map.of("username", username, "status", "PENDING"), null));
        }
    }

    private void consumeCode(String verificationId, String channel, String target, String code) {
        if (verificationId == null || !verificationId.matches("^[0-9a-fA-F-]{36}$") || code == null || !code.matches("^\\d{6}$")) {
            throw BusinessException.unauthorized("验证码错误或已过期");
        }
        String key = codeKey(verificationId), failures = "registration:verify:fail:" + verificationId;
        try {
            Long result = redis.execute(VERIFY_SCRIPT, List.of(key, failures), otpDigest(channel, target, code),
                    Long.toString(boundedTtl()), "5");
            if (result == null || result <= 0) {
                if (result == null || result == 0 || result <= -5) notifications.forgetVerificationCode(verificationId);
                audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_VERIFY_FAILED", "AUTH",
                        "注册验证码校验失败", Map.of("verificationId", verificationId, "channel", channel,
                                "target", mask(target), "attemptState", result == null ? "UNAVAILABLE" : result));
                throw BusinessException.unauthorized("验证码错误或已过期");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_VERIFY_FAILED", "AUTH",
                    "注册验证码校验因Redis不可用被拒绝", Map.of("verificationId", verificationId));
            throw new BusinessException(503, "验证码服务暂不可用，请稍后重试");
        }
    }

    private void enforceRate(String key, int configuredLimit) {
        int limit = Math.max(1, configuredLimit);
        try {
            Long allowed = redis.execute(RATE_SCRIPT, List.of(key), Long.toString(Math.max(60, config().rateWindowSeconds())), Integer.toString(limit));
            if (allowed == null) throw new IllegalStateException("Redis rate script returned null");
            if (allowed == 0) throw new BusinessException(429, "验证码请求过多，请稍后重试");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            audit.recordIndependent(null, "anonymous", "REGISTRATION_CODE_FAILED", "AUTH",
                    "注册验证码因Redis限流服务不可用被拒绝", Map.of("rateScope", rateScope(key)));
            throw new BusinessException(503, "验证码服务暂不可用，请稍后重试");
        }
    }

    private RdpProperties.Registration config() {
        if (properties.registration() == null) throw new IllegalStateException("Registration configuration is missing");
        return properties.registration();
    }
    private void requireEnabled() {
        if (!config().enabled()) throw BusinessException.forbidden("平台未开放自助注册");
        if (!notifications.adapterCode().equalsIgnoreCase(config().notificationAdapter())) {
            throw new BusinessException(503, "注册通知适配器尚未配置");
        }
    }
    private List<String> channels() {
        if (config().channels() == null) return List.of();
        return Arrays.stream(config().channels().split(",")).map(String::trim).map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> value.equals("EMAIL") || value.equals("PHONE")).distinct().toList();
    }
    private String normalizeChannel(String channel) {
        String value = channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
        if (!channels().contains(value)) throw BusinessException.badRequest("不支持该验证码渠道");
        return value;
    }
    private String normalizeTarget(String channel, String value) {
        String target = value == null ? "" : value.trim();
        if ("EMAIL".equals(channel)) {
            target = target.toLowerCase(Locale.ROOT);
            if (!target.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || target.length() > 100) throw BusinessException.badRequest("邮箱格式不正确");
        } else if (!target.matches("^1\\d{10}$")) throw BusinessException.badRequest("手机号格式不正确");
        return target;
    }
    private String otpDigest(String channel, String target, String code) { return hmac("REGISTER|" + channel + "|" + target + "|" + code); }
    private String destinationDigest(String channel, String target) { return hmac("REGISTRATION_DESTINATION|" + channel + "|" + target); }
    private void requireVerifiedDestination(Map<String,Object> application) {
        String channel = String.valueOf(application.get("channel"));
        Object rawTarget = application.get("target");
        if (rawTarget == null) throw new BusinessException(409, "已验证联系方式已被修改，请重新提交注册申请");
        String target;
        try { target = normalizeTarget(channel, rawTarget.toString()); }
        catch (BusinessException ex) { throw new BusinessException(409, "已验证联系方式已被修改，请重新提交注册申请"); }
        byte[] expected = String.valueOf(application.get("destinationDigest")).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = destinationDigest(channel, target).getBytes(StandardCharsets.US_ASCII);
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(409, "已验证联系方式已被修改，请重新提交注册申请");
        }
    }
    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.security().dataEncryptionKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private String codeKey(String verificationId) { return "registration:verify:" + verificationId; }
    private String rateScope(String key){if(key==null)return "UNKNOWN";if(key.equals("registration:rate:global"))return "GLOBAL";return key.startsWith("registration:rate:target:")?"TARGET":key.startsWith("registration:rate:ip:")?"IP":"UNKNOWN";}
    private void adminMutationLock(){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(34882201)) x").query(Long.class).single();}
    private long boundedTtl() { return Math.max(60, Math.min(600, config().codeTtlSeconds())); }
    private long boundedResend() { return Math.max(30, Math.min(300, config().resendSeconds())); }
    private String mask(String value) {
        if (value == null || value.isBlank()) return "***";
        int at = value.indexOf('@');
        if (at > 0) return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
        return value.length() >= 7 ? value.substring(0,3) + "****" + value.substring(value.length()-4) : "***";
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<?> array(Array array) throws java.sql.SQLException { return array == null ? List.of() : Arrays.asList((Object[])array.getArray()); }
    public record CodeRequest(String channel, String target, String captchaKey, String captchaCode) {}
    public record CodeDispatch(String verificationId, long expiresInSeconds, long resendAfterSeconds, String destinationMasked) {}
    public record RegisterRequest(String username, String realName, String email, String phone, String password,
                                  String confirmPassword, String verificationChannel, String verificationId,
                                  String verificationCode, String captchaKey, String captchaCode) {}
    public record ReviewRequest(String decision, String comment, List<Long> roleIds, List<Long> scopeIds,
                                Long departmentId, int applicationVersion, int authorizationVersion) {}
}
