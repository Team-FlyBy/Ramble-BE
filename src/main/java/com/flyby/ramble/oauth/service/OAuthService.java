package com.flyby.ramble.oauth.service;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.service.JwtService;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.oauth.client.AppleOAuthClient;
import com.flyby.ramble.oauth.client.GoogleOAuthClient;
import com.flyby.ramble.oauth.dto.*;
import com.flyby.ramble.oauth.util.OidcTokenParser;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.oauth.dto.OAuthRevokeInfo;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {
    private final UserService userService;
    private final JwtService jwtService;

    private final GoogleOAuthClient googleOAuthClient;
    private final AppleOAuthClient appleOAuthClient;
    private final OidcTokenParser oidcTokenParser;

    private final ClientRegistrationRepository clientRegistrationRepo;

    // Authorization Code Flow + OIDC + PKCE
    public Tokens authenticateWithAuthCode(OAuthProvider provider, OAuthPkceDTO request, DeviceType deviceType) {
        OAuth2AccessTokenResponse tokenResponse = getTokenResponse(provider, request, deviceType);

        OAuth2AccessToken accessToken = tokenResponse.getAccessToken();
        OAuth2RefreshToken refreshToken = tokenResponse.getRefreshToken();
        Object idTokenObj = tokenResponse.getAdditionalParameters().get("id_token");
        if (idTokenObj == null) {
            throw new IllegalStateException("ID token not found in token response");
        }

        OidcTokenInfo tokenInfo = oidcTokenParser.parseIdToken(provider, idTokenObj.toString());
        String oauthRefreshToken = refreshToken != null ? refreshToken.getTokenValue() : null;
        Supplier<OAuthPersonInfo> supplier = () -> resolvePersonInfo(provider, accessToken);

        UserInfoDTO user = userService.findOrCreateUser(tokenInfo, supplier, oauthRefreshToken);

        return jwtService.generateTokens(user, deviceType);
    }

    // Apple Native Flow
    public Tokens authenticateWithAppleIdToken(AppleNativeAuthDTO request, DeviceType deviceType) {
        AppleTokenResponse appleResponse = appleOAuthClient.exchangeAuthorizationCode(request.authorizationCode());

        OidcTokenInfo tokenInfo = oidcTokenParser.parseAppleNativeIdToken(request.identityToken(), request.email(), request.name());
        String refreshTokenValue = appleResponse.refreshToken();
        Supplier<OAuthPersonInfo> supplier = () -> new OAuthPersonInfo(Gender.UNKNOWN, null);

        UserInfoDTO user = userService.findOrCreateUser(tokenInfo, supplier, refreshTokenValue);

        return jwtService.generateTokens(user, deviceType);
    }

    // oauth 계정 연결 삭제
    public void withdraw(String userExternalId) {
        OAuthRevokeInfo revokeInfo = userService.withdraw(userExternalId);

        try {
            switch (revokeInfo.provider()) {
                case GOOGLE -> googleOAuthClient.revokeToken(revokeInfo.oauthRefreshToken());
                case APPLE  -> appleOAuthClient.revokeToken(revokeInfo.oauthRefreshToken());
            }
        } catch (Exception e) {
            // oauth 연결 해제가 실패해도 탈퇴 과정은 진행되어야 함
            log.warn("OAuth token revoke failed during withdrawal (provider={}): {}",
                    revokeInfo.provider(), e.getMessage());
        }
    }

    private OAuth2AccessTokenResponse getTokenResponse(
            OAuthProvider provider,
            OAuthPkceDTO request,
            DeviceType deviceType
    ) {
        ClientRegistration registration = resolveRegistration(provider, deviceType);

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                registration,
                new OAuth2AuthorizationExchange(
                        OAuth2AuthorizationRequest.authorizationCode()
                                .clientId(registration.getClientId())
                                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                                .redirectUri(request.redirectUri())
                                .attributes(Map.of(PkceParameterNames.CODE_VERIFIER, request.codeVerifier()))
                                .build(),
                        OAuth2AuthorizationResponse.success(request.code())
                                .redirectUri(request.redirectUri())
                                .build()
                )
        );

        RestClientAuthorizationCodeTokenResponseClient accessTokenResponseClient =
                new RestClientAuthorizationCodeTokenResponseClient();

        // Apple일 경우 client_secret 동적 주입
        if (provider.equals(OAuthProvider.APPLE)) {
            accessTokenResponseClient.setParametersCustomizer(parameters -> {
                String appleClientSecret = appleOAuthClient.createAppleClientSecret(registration.getClientId());
                parameters.set("client_secret", appleClientSecret);
            });
        }

        return accessTokenResponseClient.getTokenResponse(grantRequest);
    }

    private ClientRegistration resolveRegistration(OAuthProvider provider, DeviceType deviceType) {
        String base = provider.name().toLowerCase();
        String registrationId = provider == OAuthProvider.APPLE ? base : base + "-" + deviceType.name().toLowerCase();
        ClientRegistration registration = clientRegistrationRepo.findByRegistrationId(registrationId);

        if (registration == null) {
            throw new IllegalStateException(provider.name() + " OAuth " + registrationId + " not found.");
        }

        return registration;
    }

    private OAuthPersonInfo resolvePersonInfo(OAuthProvider provider, OAuth2AccessToken accessToken) {
        return switch (provider) {
            case GOOGLE -> googleOAuthClient.getPersonInfo(accessToken);
            case APPLE -> new OAuthPersonInfo(Gender.UNKNOWN, null);
        };
    }

}
