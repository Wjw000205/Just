package org.example.just.service.oa;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class OaRestOAuthClient implements OaOAuthClient {

    private final OaAuthProperties properties;
    private final RestTemplate restTemplate;

    public OaRestOAuthClient(OaAuthProperties properties) {
        this(properties, new RestTemplate());
    }

    OaRestOAuthClient(OaAuthProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public OaTokenResponse exchangeCode(OaTokenRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grantType", "authorization_code");
        body.add("code", request.code());
        body.add("clientId", request.clientId());
        body.add("clientSecret", request.clientSecret());
        body.add("redirectUri", request.redirectUri());
        body.add("code_verifier", request.codeVerifier());

        JsonNode json = restTemplate.postForObject(
                properties.oauthUrl("/oauth/token"),
                formEntity(body),
                JsonNode.class
        );
        ensureSuccess(json, "获取 OA accessToken 失败");

        return new OaTokenResponse(
                text(json, "accessToken"),
                text(json, "tokenType"),
                longValue(json, "expiresIn"),
                text(json, "refreshToken")
        );
    }

    @Override
    public OaUserInfo fetchUserInfo(String tokenType, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, authorizationValue(tokenType, accessToken));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("access_token", accessToken);

        JsonNode json = restTemplate.postForObject(
                properties.oauthUrl("/oauth/userinfo"),
                new HttpEntity<>(body, headers),
                JsonNode.class
        );
        ensureSuccess(json, "获取 OA 用户信息失败");

        JsonNode user = json.has("userAttribute") && json.get("userAttribute").isObject()
                ? json.get("userAttribute")
                : json;
        return new OaUserInfo(
                firstText(user, "userId", "id"),
                firstText(user, "username", "userName"),
                firstText(user, "name", "nickName", "realName"),
                firstText(user, "email"),
                firstText(user, "telephone", "phone", "mobile")
        );
    }

    private HttpEntity<MultiValueMap<String, String>> formEntity(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(body, headers);
    }

    private void ensureSuccess(JsonNode json, String fallbackMessage) {
        if (json == null) {
            throw new IllegalStateException(fallbackMessage);
        }
        if (json.has("code") && json.get("code").asInt() != 200) {
            String message = text(json, "msg");
            if (!StringUtils.hasText(message)) {
                message = text(json, "message");
            }
            throw new IllegalStateException(StringUtils.hasText(message) ? message : fallbackMessage);
        }
    }

    private String authorizationValue(String tokenType, String accessToken) {
        if (StringUtils.hasText(tokenType)) {
            return tokenType.trim() + " " + accessToken;
        }
        return accessToken;
    }

    private String firstText(JsonNode json, String... names) {
        for (String name : names) {
            String value = text(json, name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String text(JsonNode json, String name) {
        if (json == null || !json.has(name) || json.get(name).isNull()) {
            return "";
        }
        return json.get(name).asText("");
    }

    private long longValue(JsonNode json, String name) {
        if (json == null || !json.has(name) || json.get(name).isNull()) {
            return 0;
        }
        return json.get(name).asLong();
    }
}
