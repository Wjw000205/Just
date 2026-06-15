package org.example.just.service.oa;

import cn.hutool.crypto.SmUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OaPkceServiceTest {

    @Test
    void codeVerifierUsesPkceAllowedLengthAndCharacters() {
        OaPkceService service = new OaPkceService(properties());

        String verifier = service.generateCodeVerifier();

        assertThat(verifier).hasSizeBetween(43, 128);
        assertThat(verifier).matches("[A-Za-z0-9._~-]+");
    }

    @Test
    void codeChallengeUsesSm4WithFirst16ClientSecretCharacters() {
        OaAuthProperties properties = properties();
        OaPkceService service = new OaPkceService(properties);
        String verifier = "03bc8c8d-c311-4697-9509-8b2c26c3c8b0";
        String expected = SmUtil.sm4("1234567890abcdef".getBytes(StandardCharsets.UTF_8))
                .encryptBase64(verifier);

        String challenge = service.createCodeChallenge(verifier);

        assertThat(challenge).isEqualTo(expected);
    }

    private OaAuthProperties properties() {
        OaAuthProperties properties = new OaAuthProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://oa.example.com");
        properties.setClientId("CID_test");
        properties.setClientSecret("1234567890abcdef1234567890abcdef");
        properties.setRedirectUri("http://localhost:8083/oa/callback");
        properties.setFrontSuccessUri("http://localhost:5173/");
        return properties;
    }
}
