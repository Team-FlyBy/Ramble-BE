package com.flyby.ramble.oauth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.annotation.SwaggerApi;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.oauth.dto.AppleNativeAuthDTO;
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
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> googleLogin(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                            @Valid @RequestBody OAuthPkceDTO request) {
        Tokens tokens = oAuthService.authenticateWithAuthCode(OAuthProvider.GOOGLE, request, DeviceType.from(deviceType));

        return buildTokenResponse(tokens);
    }

    @PostMapping("/authorize/apple")
    @SwaggerApi(
            summary = "애플 OAuth Web 로그인",
            description = "web 애플 OAuth 2.0 인증을 통해 로그인 또는 회원가입을 처리하는 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> appleLogin(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                           @Valid @RequestBody OAuthPkceDTO request) {
        Tokens tokens = oAuthService.authenticateWithAuthCode(OAuthProvider.APPLE, request, DeviceType.from(deviceType));

        return buildTokenResponse(tokens);
    }

    @PostMapping("/authorize/apple/native")
    @SwaggerApi(
            summary = "애플 OAuth Native 로그인",
            description = "ios 기기 애플 OAuth 2.0 인증을 통해 로그인 또는 회원가입을 처리하는 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> appleNativeLogin(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                                 @Valid @RequestBody AppleNativeAuthDTO request) {
        Tokens tokens = oAuthService.authenticateWithAppleIdToken(request, DeviceType.from(deviceType));

        return buildTokenResponse(tokens);
    }

    private ResponseEntity<Void> buildTokenResponse(Tokens tokens) {
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

}
