package com.flyby.ramble.auth.controller;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.service.AuthService;
import com.flyby.ramble.auth.service.JwtService;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.common.annotation.SwaggerApi;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.oauth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final OAuthService oauthService;
    private final JwtService  jwtService;
    private final CookieUtil  cookieUtil;

    @PostMapping("/reissue")
    @SwaggerApi(
            summary = "토큰 재발급",
            description = "Access Token과 Refresh Token을 재발급하는 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> reissueToken(@RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                             @CookieValue(JwtConstants.REFRESH_COOKIE) String cookie) {
        Tokens tokens   = jwtService.reissueTokens(cookie, DeviceType.from(deviceType));
        String accToken = JwtConstants.TOKEN_PREFIX + tokens.accToken();
        String refToken = cookieUtil.createResponseCookie(tokens.refToken()).toString();

        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, accToken)
                .header(HttpHeaders.SET_COOKIE,    refToken)
                .build();
    }

    @PostMapping("/logout")
    @SwaggerApi(
            summary = "로그아웃",
            description = "로그아웃 처리 및 토큰 무효화를 수행하는 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String header,
                                       @RequestHeader(JwtConstants.HEADER_DEVICE_TYPE) String deviceType,
                                       @AuthenticationPrincipal UserDetails user) {
        String accToken = extractToken(header);
        String refToken = cookieUtil.deleteResponseCookie().toString();

        jwtService.revokeAllRefreshTokenByUserAndDevice(user.getUsername(), DeviceType.from(deviceType));
        authService.putBlackList(accToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refToken)
                .build();
    }

    // 회원 탈퇴
    @PostMapping("/withdraw")
    @SwaggerApi(
            summary = "회원 탈퇴",
            description = "회원 탈퇴 처리 및 관련 데이터 삭제를 수행하는 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    public ResponseEntity<Void> withdraw(@RequestHeader(HttpHeaders.AUTHORIZATION) String header,
                                         @AuthenticationPrincipal UserDetails user) {
        String accToken = extractToken(header);
        String refToken = cookieUtil.deleteResponseCookie().toString();

        authService.putBlackList(accToken);
        oauthService.withdraw(user.getUsername());
        jwtService.revokeAllRefreshTokenByUser(user.getUsername());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refToken)
                .build();
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(JwtConstants.TOKEN_PREFIX.length()).trim();
    }

}
