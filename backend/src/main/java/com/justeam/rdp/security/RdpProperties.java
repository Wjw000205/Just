package com.justeam.rdp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdp")
public record RdpProperties(Security security, Cors cors, Registration registration) {
    public record Security(String jwtSecret, String auditSecret, String auditKeyId, String auditPreviousKeys,
                           String dataEncryptionKey, long accessTokenSeconds, long refreshTokenSeconds,
                           boolean secureCookies, boolean trustProxyHeaders, long authRateWindowSeconds,
                           int captchaIpRateLimit, int captchaGlobalRateLimit,
                           int loginIpRateLimit, int loginGlobalRateLimit) {}
    public record Cors(String allowedOrigins) {}
    public record Registration(boolean enabled, String channels, String notificationAdapter,
                               long codeTtlSeconds, long resendSeconds, long rateWindowSeconds,
                               int targetRateLimit, int ipRateLimit, int globalRateLimit) {}
}
