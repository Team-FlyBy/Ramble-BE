package com.flyby.ramble.oauth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.annotation.SwaggerApi;
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
    @SwaggerApi(
            summary = "구글 OAuth 로그인",
            description = "구글 OAuth 2.0 인증을 통해 로그인 또는 회원가입을 처리하는 API",
            responseCode = "204",
            responseDescription = "No Content",
            content = {}
    )
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

    /**
     * @deprecated 추후 삭제 예정. 클라이언트 수정 후 제거
     */
    @Deprecated(since = "2025-08-27", forRemoval = true)
    @SwaggerApi(
            summary = "구글 OAuth 모바일 로그인 (Deprecated)",
            description = "구글 OAuth 2.0 인증을 통해 로그인 또는 회원가입을 처리하는 API (모바일)",
            responseCode = "204",
            responseDescription = "No Content",
            content = {}
    )
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
