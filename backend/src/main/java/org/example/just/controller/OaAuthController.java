package org.example.just.controller;

import lombok.RequiredArgsConstructor;
import org.example.just.service.oa.OaAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oa")
public class OaAuthController {

    private final OaAuthService oaAuthService;

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        try {
            return redirect(oaAuthService.buildAuthorizeRedirectUri());
        } catch (RuntimeException ex) {
            return redirect(oaAuthService.buildFrontErrorUri(ex.getMessage()));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam(required = false) String oaState,
            @RequestParam(required = false) String state
    ) {
        try {
            String token = oaAuthService.loginByCode(code, oaState != null ? oaState : state);
            return redirect(oaAuthService.buildFrontSuccessUri(token));
        } catch (RuntimeException ex) {
            return redirect(oaAuthService.buildFrontErrorUri(ex.getMessage()));
        }
    }

    private ResponseEntity<Void> redirect(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
