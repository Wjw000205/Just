package org.example.just.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    @Value("${jwt.secret:Just-Backend-Secret-Key-2026}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private final StringRedisTemplate redisTemplate;

    public JwtUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Integer userId, String userName, Integer role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        SecretKey key = getSigningKey();

        // 对中文用户名进行 URL 编码
        String encodedUserName = URLEncoder.encode(userName, StandardCharsets.UTF_8);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userName", encodedUserName)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(key)
                .compact();

        return token;
    }

    /**
     * 从 Token 中获取用户 Id
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? Integer.valueOf(claims.getSubject()) : null;
    }

    /**
     * 从 Token 中获取用户姓名（自动解码中文）
     */
    public String getUserNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }

        String encodedName = (String) claims.get("userName");
        if (encodedName == null || encodedName.isEmpty()) {
            return null;
        }

        // URL 解码，恢复中文
        return URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
    }

    /**
     * 从 Token 中获取角色
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (Integer) claims.get("role") : null;
    }

    /**
     * 验证 Token 是否有效（包括检查是否在黑名单中）
     */
    public boolean validateToken(String token) {
        if (!validateTokenStructure(token)) {
            return false;
        }

        if (isTokenBlacklisted(token)) {
            System.err.println("JWT 已在黑名单中");
            return false;
        }

        return true;
    }

    /**
     * 验证 Token 结构是否有效
     */
    private boolean validateTokenStructure(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            System.err.println("无效的 JWT 签名");
        } catch (ExpiredJwtException e) {
            System.err.println("JWT 已过期");
        } catch (UnsupportedJwtException e) {
            System.err.println("不支持的 JWT 格式");
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims 为空");
        }
        return false;
    }

    /**
     * 将 Token 加入黑名单（用于登出、修改密码等场景）
     */
    public void blacklistToken(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate != null) {
            long ttl = expirationDate.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set("jwt:blacklist:" + token, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token));
    }

    /**
     * 使用户的所有 Token 失效（强制下线）
     */
    public void invalidateUserToken(Integer userId) {
        String userTokenKey = "user:token:" + userId;
        String token = redisTemplate.opsForValue().get(userTokenKey);
        if (token != null) {
            blacklistToken(token);
            redisTemplate.delete(userTokenKey);
        }
    }

    /**
     * 获取 Token 中的声明
     */
    private Claims getClaimsFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 刷新 Token
     */
    public String refreshToken(String token) {
        if (!validateToken(token)) {
            return null;
        }

        Integer userId = getUserIdFromToken(token);
        String username = getUserNameFromToken(token);
        Integer role = getRoleFromToken(token);

        String newToken = generateToken(userId, username, role);
        blacklistToken(token);

        return newToken;
    }

    /**
     * 获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
}
