package org.example.just.service.oa;

import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.crypto.SmUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OaPkceService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OaAuthProperties properties;

    public String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String createCodeChallenge(String codeVerifier) {
        properties.validateForLogin();
        String clientSecret = properties.getClientSecret();
        String key = clientSecret.length() > 16 ? clientSecret.substring(0, 16) : clientSecret;
        SM4 sm4 = SmUtil.sm4(key.getBytes(StandardCharsets.UTF_8));
        return sm4.encryptBase64(codeVerifier);
    }
}
