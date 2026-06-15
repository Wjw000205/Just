package org.example.just.service.oa;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Data
@Component
@ConfigurationProperties(prefix = "oa.auth")
public class OaAuthProperties {

    private boolean enabled = false;
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontSuccessUri = "/";
    private long stateTtlSeconds = 120;
    private String stateTransport = "redirect-uri";

    public void validateForLogin() {
        if (!enabled) {
            throw new IllegalStateException("OA 统一认证未启用");
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("OA 统一认证 baseUrl 未配置");
        }
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException("OA 统一认证 clientId 未配置");
        }
        if (!StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException("OA 统一认证 clientSecret 未配置");
        }
        if (clientSecret.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException("OA 统一认证 clientSecret 长度不能小于 16 位");
        }
        if (!StringUtils.hasText(redirectUri)) {
            throw new IllegalStateException("OA 统一认证 redirectUri 未配置");
        }
        if (!StringUtils.hasText(frontSuccessUri)) {
            throw new IllegalStateException("OA 登录成功跳转地址未配置");
        }
    }

    public String oauthUrl(String path) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return root + normalizedPath;
    }

    public boolean useOauthStateParameter() {
        return "oauth-state".equalsIgnoreCase(stateTransport);
    }
}
