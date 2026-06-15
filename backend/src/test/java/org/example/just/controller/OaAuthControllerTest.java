package org.example.just.controller;

import org.example.just.service.oa.OaAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OaAuthControllerTest {

    @Test
    void callbackAcceptsStandardStateParameter() throws Exception {
        OaAuthService service = mock(OaAuthService.class);
        when(service.loginByCode("OC-1", "state-1")).thenReturn("local.jwt");
        when(service.buildFrontSuccessUri("local.jwt")).thenReturn(URI.create("http://front.example.com/?oaToken=local.jwt"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OaAuthController(service))
                .build();

        mockMvc.perform(get("/oa/callback")
                        .param("code", "OC-1")
                        .param("state", "state-1"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://front.example.com/?oaToken=local.jwt"));

        verify(service).loginByCode("OC-1", "state-1");
    }

    @Test
    void loginRedirectsToFrontendErrorWhenConfigurationIsInvalid() throws Exception {
        OaAuthService service = mock(OaAuthService.class);
        when(service.buildAuthorizeRedirectUri()).thenThrow(new IllegalStateException("OA auth disabled"));
        when(service.buildFrontErrorUri("OA auth disabled")).thenReturn(URI.create("http://front.example.com/?oaError=OA%20auth%20disabled"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OaAuthController(service))
                .build();

        mockMvc.perform(get("/oa/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://front.example.com/?oaError=OA%20auth%20disabled"));
    }
}
