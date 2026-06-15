package org.example.just.service.oa;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OaRestOAuthClientTest {

    @Test
    void userInfoRequestSendsAuthorizationHeaderAndAccessTokenFormBody() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("https://oa.example.com/oauth/userinfo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer AT-1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("access_token=AT-1"))
                .andRespond(withSuccess("""
                        {"userId":"10001","username":"oa_admin","name":"OA Admin","email":"oa@example.com"}
                        """, MediaType.APPLICATION_JSON));

        OaRestOAuthClient client = new OaRestOAuthClient(properties(), restTemplate);

        OaUserInfo userInfo = client.fetchUserInfo("Bearer", "AT-1");

        assertThat(userInfo.userId()).isEqualTo("10001");
        assertThat(userInfo.username()).isEqualTo("oa_admin");
        assertThat(userInfo.name()).isEqualTo("OA Admin");
        assertThat(userInfo.email()).isEqualTo("oa@example.com");
        server.verify();
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
