package com.flyby.ramble.oauth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.oauth.dto.OAuthIdTokenDTO;
import com.flyby.ramble.oauth.dto.OAuthPkceDTO;
import com.flyby.ramble.oauth.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final CookieUtil cookieUtil;

    @PostMapping("/authorize/google")
    public ResponseEntity<Void> login(@Valid @RequestBody OAuthPkceDTO request) {
        Tokens tokens   = oAuthService.getTokensFromGoogleUser(request);
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

    @PostMapping("/authorize/google/mobile")
    public ResponseEntity<Void> login(@Valid @RequestBody OAuthIdTokenDTO request) {
        Tokens tokens   = oAuthService.getTokensFromGoogleIdToken(request);
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

}
