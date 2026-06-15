package org.example.just.service.oa;

public interface OaOAuthClient {

    OaTokenResponse exchangeCode(OaTokenRequest request);

    OaUserInfo fetchUserInfo(String tokenType, String accessToken);
}
