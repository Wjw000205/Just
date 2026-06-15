package org.example.just.service.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.just.dao.UserDao;
import org.example.just.entity.UserEntity;
import org.example.just.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OaAuthServiceTest {

    @Test
    void authorizeRedirectStoresVerifierAndUsesRedirectUriWithState() {
        OaAuthProperties properties = properties();
        FakeStateStore stateStore = new FakeStateStore();
        OaPkceService pkceService = mock(OaPkceService.class);
        when(pkceService.generateCodeVerifier()).thenReturn("verifier-123456789012345678901234567890123456789");
        when(pkceService.createCodeChallenge("verifier-123456789012345678901234567890123456789"))
                .thenReturn("challenge+value/encoded");

        OaAuthService service = new OaAuthService(
                properties,
                stateStore,
                pkceService,
                mock(OaOAuthClient.class),
                mock(UserDao.class),
                new BCryptPasswordEncoder(),
                mock(JwtUtil.class)
        );

        URI redirect = service.buildAuthorizeRedirectUri();

        String query = redirect.getRawQuery();
        assertThat(redirect.toString()).startsWith("https://oa.example.com/oauth/authorize?");
        assertThat(query).contains("responseType=code");
        assertThat(query).contains("clientId=CID_test");
        assertThat(query).contains("code_challenge=challenge%2Bvalue%2Fencoded");
        assertThat(query).contains("code_challenge_method=SM4");
        assertThat(stateStore.savedState).isNotBlank();
        assertThat(stateStore.savedVerifier).isEqualTo("verifier-123456789012345678901234567890123456789");
        assertThat(stateStore.savedRedirectUri).isEqualTo("http://localhost:8083/oa/callback?oaState=" + stateStore.savedState);
        assertThat(decodedQueryParam(query, "redirectUri"))
                .isEqualTo("http://localhost:8083/oa/callback?oaState=" + stateStore.savedState);
    }

    @Test
    void authorizeRedirectCanUseStandardOauthStateWithFixedRedirectUri() {
        OaAuthProperties properties = properties();
        properties.setStateTransport("oauth-state");
        FakeStateStore stateStore = new FakeStateStore();
        OaPkceService pkceService = mock(OaPkceService.class);
        when(pkceService.generateCodeVerifier()).thenReturn("verifier-123456789012345678901234567890123456789");
        when(pkceService.createCodeChallenge("verifier-123456789012345678901234567890123456789"))
                .thenReturn("challenge+value/encoded");

        OaAuthService service = new OaAuthService(
                properties,
                stateStore,
                pkceService,
                mock(OaOAuthClient.class),
                mock(UserDao.class),
                new BCryptPasswordEncoder(),
                mock(JwtUtil.class)
        );

        URI redirect = service.buildAuthorizeRedirectUri();

        String query = redirect.getRawQuery();
        assertThat(query).contains("state=" + stateStore.savedState);
        assertThat(stateStore.savedRedirectUri).isEqualTo("http://localhost:8083/oa/callback");
        assertThat(decodedQueryParam(query, "redirectUri"))
                .isEqualTo("http://localhost:8083/oa/callback");
    }

    @Test
    void callbackExchangesCodeFetchesUserCreatesMissingLocalUserAndReturnsJwt() {
        OaAuthProperties properties = properties();
        FakeStateStore stateStore = new FakeStateStore();
        stateStore.stateToReturn = new OaAuthState("plain-verifier", "http://localhost:8083/oa/callback?oaState=state-1");
        OaOAuthClient oauthClient = mock(OaOAuthClient.class);
        when(oauthClient.exchangeCode(any())).thenReturn(new OaTokenResponse("AT-1", "Bearer", 43200, "RT-1"));
        when(oauthClient.fetchUserInfo("Bearer", "AT-1")).thenReturn(new OaUserInfo("10001", "oa_admin", "OA Admin", "oa@example.com", "13800000000"));

        UserDao userDao = mock(UserDao.class);
        when(userDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(9);
            return 1;
        }).when(userDao).insert(any(UserEntity.class));

        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.generateToken(9, "oa_admin", 0)).thenReturn("local.jwt.token");

        OaAuthService service = new OaAuthService(
                properties,
                stateStore,
                mock(OaPkceService.class),
                oauthClient,
                userDao,
                new BCryptPasswordEncoder(),
                jwtUtil
        );

        String token = service.loginByCode("OC-1", "state-1");

        assertThat(token).isEqualTo("local.jwt.token");
        ArgumentCaptor<OaTokenRequest> requestCaptor = ArgumentCaptor.forClass(OaTokenRequest.class);
        verify(oauthClient).exchangeCode(requestCaptor.capture());
        assertThat(requestCaptor.getValue().code()).isEqualTo("OC-1");
        assertThat(requestCaptor.getValue().clientId()).isEqualTo("CID_test");
        assertThat(requestCaptor.getValue().clientSecret()).isEqualTo("1234567890abcdef1234567890abcdef");
        assertThat(requestCaptor.getValue().redirectUri()).isEqualTo("http://localhost:8083/oa/callback?oaState=state-1");
        assertThat(requestCaptor.getValue().codeVerifier()).isEqualTo("plain-verifier");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userDao).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("oa_admin");
        assertThat(userCaptor.getValue().getRealName()).isEqualTo("OA Admin");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("oa@example.com");
        assertThat(userCaptor.getValue().getTelephone()).isEqualTo("13800000000");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(0);
        assertThat(userCaptor.getValue().getDeleted()).isEqualTo(0);
        assertThat(userCaptor.getValue().getPassword()).isNotBlank();
        assertThat(stateStore.consumedState).isEqualTo("state-1");
    }

    private static OaAuthProperties properties() {
        OaAuthProperties properties = new OaAuthProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://oa.example.com");
        properties.setClientId("CID_test");
        properties.setClientSecret("1234567890abcdef1234567890abcdef");
        properties.setRedirectUri("http://localhost:8083/oa/callback");
        properties.setFrontSuccessUri("http://localhost:5173/");
        return properties;
    }

    private static String decodedQueryParam(String rawQuery, String name) {
        for (String part : rawQuery.split("&")) {
            int index = part.indexOf('=');
            if (index > 0 && part.substring(0, index).equals(name)) {
                return URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static final class FakeStateStore implements OaAuthStateStore {
        private String savedState;
        private String savedVerifier;
        private String savedRedirectUri;
        private String consumedState;
        private OaAuthState stateToReturn;

        @Override
        public void save(String state, OaAuthState authState) {
            this.savedState = state;
            this.savedVerifier = authState.codeVerifier();
            this.savedRedirectUri = authState.redirectUri();
        }

        @Override
        public OaAuthState consume(String state) {
            this.consumedState = state;
            return stateToReturn;
        }
    }
}
