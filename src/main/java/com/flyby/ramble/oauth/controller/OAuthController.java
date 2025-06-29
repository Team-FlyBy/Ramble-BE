package com.flyby.ramble.oauth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.oauth.dto.OAuthRequestDTO;
import com.flyby.ramble.oauth.service.OAuthService;
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

    @PostMapping("/login/google")
    public ResponseEntity<Void> login(@RequestBody OAuthRequestDTO request) {
        Tokens tokens   = oAuthService.getGoogleUserInfo(request);
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

}
