package org.example.just.service.oa;

public record OaTokenRequest(
        String code,
        String clientId,
        String clientSecret,
        String redirectUri,
        String codeVerifier
) {
}
