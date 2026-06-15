package org.example.just.service.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.just.dao.UserDao;
import org.example.just.entity.UserEntity;
import org.example.just.utils.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaAuthService {

    private final OaAuthProperties properties;
    private final OaAuthStateStore stateStore;
    private final OaPkceService pkceService;
    private final OaOAuthClient oauthClient;
    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public URI buildAuthorizeRedirectUri() {
        properties.validateForLogin();

        String state = UUID.randomUUID().toString().replace("-", "");
        String codeVerifier = pkceService.generateCodeVerifier();
        String redirectUriWithState = redirectUriForState(state);
        String codeChallenge = pkceService.createCodeChallenge(codeVerifier);

        stateStore.save(state, new OaAuthState(codeVerifier, redirectUriWithState));

        String authorizeUrl = properties.oauthUrl("/oauth/authorize")
                + "?responseType=" + encodeQueryParam("code")
                + "&clientId=" + encodeQueryParam(properties.getClientId())
                + "&redirectUri=" + encodeQueryParam(redirectUriWithState)
                + "&code_challenge=" + encodeQueryParam(codeChallenge)
                + "&code_challenge_method=" + encodeQueryParam("SM4")
                + oauthStateQueryPart(state);
        return URI.create(authorizeUrl);
    }

    public String loginByCode(String code, String state) {
        properties.validateForLogin();
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("OA 回调缺少 code");
        }

        OaAuthState authState = stateStore.consume(state);
        if (authState == null) {
            throw new IllegalArgumentException("OA 登录状态已过期，请重新登录");
        }

        OaTokenResponse token = oauthClient.exchangeCode(new OaTokenRequest(
                code,
                properties.getClientId(),
                properties.getClientSecret(),
                authState.redirectUri(),
                authState.codeVerifier()
        ));
        if (token == null || !StringUtils.hasText(token.accessToken())) {
            throw new IllegalStateException("OA 未返回 accessToken");
        }

        OaUserInfo userInfo = oauthClient.fetchUserInfo(token.tokenType(), token.accessToken());
        UserEntity user = findOrCreateLocalUser(userInfo);
        Integer role = user.getRole() == null ? 0 : user.getRole();
        return jwtUtil.generateToken(user.getId(), user.getUsername(), role);
    }

    public URI buildFrontSuccessUri(String localToken) {
        return UriComponentsBuilder.fromUriString(properties.getFrontSuccessUri())
                .queryParam("oaToken", localToken)
                .build()
                .encode()
                .toUri();
    }

    public URI buildFrontErrorUri(String message) {
        return UriComponentsBuilder.fromUriString(properties.getFrontSuccessUri())
                .queryParam("oaError", StringUtils.hasText(message) ? message : "OA 登录失败")
                .build()
                .encode()
                .toUri();
    }

    private UserEntity findOrCreateLocalUser(OaUserInfo userInfo) {
        String username = userInfo.normalizedUsername();

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("limit 1");
        UserEntity existing = userDao.selectOne(wrapper);
        if (existing != null) {
            updateLocalUserProfile(existing, userInfo);
            return existing;
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("OA:" + UUID.randomUUID()));
        user.setTelephone(defaultString(userInfo.telephone()));
        user.setEmail(defaultString(userInfo.email()));
        user.setRealName(userInfo.normalizedName());
        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        int rows = userDao.insert(user);
        if (rows <= 0 || user.getId() == null) {
            throw new IllegalStateException("创建 OA 本地用户失败");
        }
        return user;
    }

    private void updateLocalUserProfile(UserEntity existing, OaUserInfo userInfo) {
        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserEntity::getId, existing.getId());
        boolean hasUpdate = false;

        if (StringUtils.hasText(userInfo.name())) {
            updateWrapper.set(UserEntity::getRealName, userInfo.name().trim());
            existing.setRealName(userInfo.name().trim());
            hasUpdate = true;
        }
        if (StringUtils.hasText(userInfo.email())) {
            updateWrapper.set(UserEntity::getEmail, userInfo.email().trim());
            existing.setEmail(userInfo.email().trim());
            hasUpdate = true;
        }
        if (StringUtils.hasText(userInfo.telephone())) {
            updateWrapper.set(UserEntity::getTelephone, userInfo.telephone().trim());
            existing.setTelephone(userInfo.telephone().trim());
            hasUpdate = true;
        }

        if (hasUpdate) {
            userDao.update(null, updateWrapper);
        }
    }

    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String redirectUriForState(String state) {
        if (properties.useOauthStateParameter()) {
            return properties.getRedirectUri();
        }
        return UriComponentsBuilder.fromUriString(properties.getRedirectUri())
                .queryParam("oaState", state)
                .build()
                .toUriString();
    }

    private String oauthStateQueryPart(String state) {
        if (!properties.useOauthStateParameter()) {
            return "";
        }
        return "&state=" + encodeQueryParam(state);
    }
}
