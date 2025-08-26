package com.flyby.ramble.oauth.service;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.service.JwtService;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.oauth.dto.GooglePersonInfo;
import com.flyby.ramble.oauth.dto.OAuthIdTokenDTO;
import com.flyby.ramble.oauth.dto.OAuthRegisterDTO;
import com.flyby.ramble.oauth.dto.OAuthPkceDTO;
import com.flyby.ramble.oauth.util.OidcTokenParser;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final GooglePeopleApiService googlePeopleApiService;
    private final OidcTokenParser oidcTokenParser;

    private final ClientRegistrationRepository clientRegistrationRepo;

    // TODO: 추후 리팩토링
    // 1. 회원가입의 경우, 추가 정보(생년월일, 성별 등) 동의하면 GooglePersonInfo 요청 (현재 로그인, 회원가입 구분 X)
    // 2-1. 로그인의 경우, 유저에게 추가 정보(생년월일, 성별 등)가 있으면 GooglePersonInfo 요청 안 함.
    // 2-2. 로그인의 경우, 유저에게 추가 정보(생년월일, 성별 등)가 없으면 GooglePersonInfo 요청.
    // (이유) Google의 경우 처음에 동의 안 해도 재로그인 시 동의 창이 뜸.

    public Tokens getTokensFromGoogleUser(OAuthPkceDTO request, DeviceType deviceType) {
        OAuth2AccessTokenResponse tokenResponse = getGoogleTokenResponse(request.code(), request.codeVerifier(), request.redirectUri(), deviceType);
        OAuth2AccessToken accessToken = tokenResponse.getAccessToken();
        String idToken = tokenResponse.getAdditionalParameters().get("id_token").toString();

        // People API를 통한 추가 정보 수집
        GooglePersonInfo personInfo  = googlePeopleApiService.getPersonInfo(accessToken);
        OAuthRegisterDTO registerDTO = oidcTokenParser.parseGoogleIdToken(idToken, personInfo);
        User user = userService.registerOrLogin(registerDTO);

        return jwtService.generateTokens(user, deviceType);
    }

    /**
     * @deprecated 추후 삭제 예정. 클라이언트 수정 후 제거
     */
    @Deprecated(since = "2025-08-27", forRemoval = true)
    public Tokens getTokensFromGoogleIdToken(OAuthIdTokenDTO request, DeviceType deviceType) {
        String idToken = request.token();

        OAuthRegisterDTO registerDTO = oidcTokenParser.parseGoogleIdToken(idToken, new GooglePersonInfo(null, null));
        User user = userService.registerOrLogin(registerDTO);

        return jwtService.generateTokens(user, deviceType);
    }

    private OAuth2AccessTokenResponse getGoogleTokenResponse(String code, String codeVerifier, String redirectUri, DeviceType deviceType) {
        String registrationId = "google-" + deviceType.name().toLowerCase();
        ClientRegistration registration = clientRegistrationRepo.findByRegistrationId(registrationId);

        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient =
                new RestClientAuthorizationCodeTokenResponseClient();

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                registration,
                new OAuth2AuthorizationExchange(
                        OAuth2AuthorizationRequest.authorizationCode()
                                .clientId(registration.getClientId())
                                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                                .redirectUri(redirectUri)
                                .attributes(Map.of(PkceParameterNames.CODE_VERIFIER, codeVerifier))
                                .build(),
                        OAuth2AuthorizationResponse.success(code)
                                .redirectUri(redirectUri)
                                .build()
                )
        );

        return accessTokenResponseClient.getTokenResponse(grantRequest);
    }

}
