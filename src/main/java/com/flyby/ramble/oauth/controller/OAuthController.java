package com.flyby.ramble.oauth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.oauth.dto.OAuthIdTokenDTO;
import com.flyby.ramble.oauth.dto.OAuthPkceDTO;
import com.flyby.ramble.oauth.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final CookieUtil cookieUtil;

    @PostMapping("/authorize/google")
    public ResponseEntity<Void> login(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                      @Valid @RequestBody OAuthPkceDTO request) {
        Tokens tokens   = oAuthService.getTokensFromGoogleUser(request, DeviceType.from(deviceType));
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

    @PostMapping("/authorize/google/mobile")
    public ResponseEntity<Void> login(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                      @Valid @RequestBody OAuthIdTokenDTO request) {
        Tokens tokens   = oAuthService.getTokensFromGoogleIdToken(request, DeviceType.from(deviceType));
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

}
