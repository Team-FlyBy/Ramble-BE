package com.flyby.ramble.oauth.service;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.service.JwtService;
import com.flyby.ramble.common.model.DeviceType;
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
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final OidcTokenParser oidcTokenParser;

    private final ClientRegistrationRepository clientRegistrationRepo;

    public Tokens getTokensFromGoogleUser(OAuthPkceDTO request, DeviceType deviceType) {
        String idToken = getGoogleIdToken(request.code(), request.codeVerifier(), request.redirectUri());

        OAuthRegisterDTO registerDTO = oidcTokenParser.parseGoogleIdToken(idToken);
        User user = userService.registerOrLogin(registerDTO);

        return jwtService.generateTokens(user, deviceType);
    }

    public Tokens getTokensFromGoogleIdToken(OAuthIdTokenDTO request, DeviceType deviceType) {
        String idToken = request.token();

        OAuthRegisterDTO registerDTO = oidcTokenParser.parseGoogleIdToken(idToken);
        User user = userService.registerOrLogin(registerDTO);

        return jwtService.generateTokens(user, deviceType);
    }

    private String getGoogleIdToken(String code, String codeVerifier, String redirectUri) {
        ClientRegistration registration = clientRegistrationRepo.findByRegistrationId("google");

        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient =
                new RestClientAuthorizationCodeTokenResponseClient();

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                registration,
                new OAuth2AuthorizationExchange(
                        OAuth2AuthorizationRequest.authorizationCode()
                                .clientId(registration.getClientId())
                                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                                .redirectUri(redirectUri)
                                .attributes(Map.of(
                                        PkceParameterNames.CODE_VERIFIER, codeVerifier
                                ))
                                .build(),
                        OAuth2AuthorizationResponse.success(code)
                                .redirectUri(redirectUri)
                                .build()
                )
        );

        OAuth2AccessTokenResponse tokenResponse = accessTokenResponseClient.getTokenResponse(grantRequest);
        return tokenResponse.getAdditionalParameters().get("id_token").toString();
    }

}
