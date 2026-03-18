package com.flyby.ramble.oauth.util;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.oauth.constants.OAuthConstants;
import com.flyby.ramble.oauth.dto.OidcTokenInfo;
import com.flyby.ramble.oauth.properties.OAuthProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OidcTokenParser {
    private final OAuthProperties oAuthProperties;

    private JwtDecoder googleJwtDecoder;
    private NimbusJwtDecoder appleIosJwtDecoder;
    private NimbusJwtDecoder appleWebJwtDecoder;

    @PostConstruct
    public void init() {
        this.googleJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(OAuthConstants.GOOGLE_JWK_SET_URI)
                .build();

        this.appleIosJwtDecoder = buildAppleDecoder(oAuthProperties.getClientId());
        this.appleWebJwtDecoder = buildAppleDecoder(oAuthProperties.getServiceId());
    }

    private NimbusJwtDecoder buildAppleDecoder(String expectedClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(OAuthConstants.APPLE_JWK_SET_URI)
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(OAuthConstants.APPLE_ISSUER),
                jwt -> {
                    if (jwt.getAudience().contains(expectedClientId)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "audience 불일치: " + jwt.getAudience(), null)
                    );
                }
        ));
        return decoder;
    }

    public OidcTokenInfo parseIdToken(OAuthProvider provider, String idToken) {
        try {
            Jwt jwt = switch (provider) {
                case GOOGLE -> googleJwtDecoder.decode(idToken);
                case APPLE -> appleWebJwtDecoder.decode(idToken);
            };

            String sub   = jwt.getClaimAsString("sub");
            String email = jwt.getClaimAsString("email");
            String name  = jwt.getClaimAsString("name");

            if (sub == null) {
                throw new IllegalArgumentException("Missing required claims in " + provider + " ID token");
            }

            return new OidcTokenInfo(provider, sub, email, name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID token", e);
        }
    }

    public OidcTokenInfo parseAppleNativeIdToken(String idToken, String email, String name) {
        try {
            Jwt jwt = appleIosJwtDecoder.decode(idToken);
            String sub = jwt.getClaimAsString("sub");

            if (sub == null) {
                throw new IllegalArgumentException("Missing required claims in Apple ID token");
            }

            return new OidcTokenInfo(OAuthProvider.APPLE, sub, email, name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID token", e);
        }
    }

}
