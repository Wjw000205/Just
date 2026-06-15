package org.example.just.service.oa;

public record OaTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
}
