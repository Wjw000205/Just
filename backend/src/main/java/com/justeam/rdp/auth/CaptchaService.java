package com.justeam.rdp.auth;

import com.justeam.rdp.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CaptchaService {
    private final StringRedisTemplate redis;
    private final com.justeam.rdp.audit.AuditService audit;

    public CaptchaService(StringRedisTemplate redis, com.justeam.rdp.audit.AuditService audit) {
        this.redis = redis;
        this.audit = audit;
    }

    public Captcha create() {
        int left = ThreadLocalRandom.current().nextInt(1, 10);
        int right = ThreadLocalRandom.current().nextInt(1, 10);
        String key = UUID.randomUUID().toString();
        String answer = Integer.toString(left + right);
        try {
            redis.opsForValue().set("captcha:" + key, answer, Duration.ofMinutes(5));
        } catch (Exception ex) {
            audit.recordIndependent(null,"anonymous","CAPTCHA_SERVICE_FAILED","AUTH",
                    "图形验证码因Redis不可用未生成",java.util.Map.of("operation","CREATE"));
            throw new BusinessException(503,"验证码服务暂不可用，请稍后重试");
        }
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="140" height="44" viewBox="0 0 140 44">
                  <rect width="140" height="44" rx="8" fill="#eef4ff"/>
                  <path d="M4 32 L136 11 M7 10 L133 35" stroke="#b9c9e8" stroke-width="1"/>
                  <text x="70" y="30" text-anchor="middle" font-family="monospace" font-size="22" font-weight="700" fill="#22406f">%d + %d = ?</text>
                </svg>
                """.formatted(left, right);
        return new Captcha(key, "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)), 300);
    }

    public void verify(String key, String answer) {
        if (key == null || answer == null) throw BusinessException.badRequest("请输入验证码");
        String expected;
        try {
            expected = redis.opsForValue().getAndDelete("captcha:" + key);
        } catch (Exception ex) {
            audit.recordIndependent(null,"anonymous","CAPTCHA_SERVICE_FAILED","AUTH",
                    "图形验证码因Redis不可用未校验",java.util.Map.of("operation","VERIFY"));
            throw new BusinessException(503,"验证码服务暂不可用，请稍后重试");
        }
        if (expected == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), answer.trim().getBytes(StandardCharsets.UTF_8))) {
            audit.recordIndependent(null,"anonymous","CAPTCHA_VERIFY_FAILED","AUTH",
                    "图形验证码错误或已过期",java.util.Map.of("captchaKeyDigest",sha256(key)));
            throw BusinessException.badRequest("验证码错误或已过期");
        }
    }

    private String sha256(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){return "unavailable";}}

    public record Captcha(String captchaKey, String image, int expiresIn) {}
}
