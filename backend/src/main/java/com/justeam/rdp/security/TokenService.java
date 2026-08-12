package com.justeam.rdp.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final RdpProperties properties;
    private final SecureRandom random = new SecureRandom();

    public TokenService(ObjectMapper mapper, StringRedisTemplate redis, RdpProperties properties) {
        this.mapper = mapper;
        this.redis = redis;
        this.properties = properties;
    }

    public TokenPair issue(UserPrincipal user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", Long.toString(user.id()));
        claims.put("username", user.username());
        claims.put("name", user.realName());
        claims.put("roles", user.roles());
        claims.put("permissions", user.permissions());
        claims.put("scopes", user.dataScopes());
        claims.put("jti", jti);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(properties.security().accessTokenSeconds()).getEpochSecond());
        claims.put("typ", "access");
        String access = encode(claims);
        byte[] refreshBytes = new byte[48];
        random.nextBytes(refreshBytes);
        String refresh = URL_ENCODER.encodeToString(refreshBytes);
        Duration accessTtl = Duration.ofSeconds(properties.security().accessTokenSeconds());
        Duration refreshTtl = Duration.ofSeconds(properties.security().refreshTokenSeconds());
        redis.opsForValue().set("session:" + user.id(), jti, accessTtl);
        redis.opsForValue().set("refresh:" + sha256(refresh), user.id() + ":" + jti, refreshTtl);
        return new TokenPair(access, refresh, properties.security().accessTokenSeconds());
    }

    public long consumeRefresh(String refreshToken) {
        String key = "refresh:" + sha256(refreshToken);
        String value = redis.opsForValue().getAndDelete(key);
        if (value == null) {
            throw BusinessException.unauthorized("刷新令牌无效或已过期");
        }
        String[] parts = value.split(":", 2);
        String activeJti = redis.opsForValue().get("session:" + parts[0]);
        if (activeJti == null || !MessageDigest.isEqual(activeJti.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.unauthorized("登录会话已失效");
        }
        return Long.parseLong(parts[0]);
    }

    public Claims validateAccess(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("token segments");
            String signed = parts[0] + "." + parts[1];
            byte[] expected = hmac(signed);
            byte[] actual = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) throw new IllegalArgumentException("signature");
            Map<String, Object> payload = mapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) throw new IllegalArgumentException("expired");
            long userId = Long.parseLong(payload.get("sub").toString());
            String jti = payload.get("jti").toString();
            String active = redis.opsForValue().get("session:" + userId);
            if (active == null || !MessageDigest.isEqual(active.getBytes(StandardCharsets.UTF_8), jti.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("session");
            }
            return new Claims(userId, payload.get("username").toString(), payload.get("name").toString(),
                    stringList(payload.get("roles")), stringList(payload.get("permissions")), longList(payload.get("scopes")), jti);
        } catch (Exception ex) {
            throw BusinessException.unauthorized("访问令牌无效或已过期");
        }
    }

    public void revoke(long userId) {
        redis.delete("session:" + userId);
    }

    private String encode(Map<String, Object> claims) {
        try {
            String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = URL_ENCODER.encodeToString(mapper.writeValueAsBytes(claims));
            String signed = header + "." + payload;
            return signed + "." + URL_ENCODER.encodeToString(hmac(signed));
        } catch (Exception ex) {
            throw new IllegalStateException("生成访问令牌失败", ex);
        }
    }

    private byte[] hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        return value == null ? List.of() : ((List<Object>) value).stream().map(Object::toString).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> longList(Object value) {
        return value == null ? List.of() : ((List<Object>) value).stream().map(v -> ((Number) v).longValue()).toList();
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}
    public record Claims(long userId, String username, String realName, List<String> roles,
                         List<String> permissions, List<Long> dataScopes, String jti) {}
}
