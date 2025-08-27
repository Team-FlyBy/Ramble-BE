package com.flyby.ramble.oauth.util;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.oauth.dto.GooglePersonInfo;
import com.flyby.ramble.oauth.dto.OAuthRegisterDTO;
import com.flyby.ramble.user.model.Gender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class OidcTokenParser {

    // TODO: custom 예외 처리 추가

    private JwtDecoder jwtDecoder;

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    @PostConstruct
    public void init() {
        this.jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWK_SET_URI)
                .build();
    }

    public OAuthRegisterDTO parseGoogleIdToken(String idToken, GooglePersonInfo personInfo) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            String sub    = jwt.getClaimAsString("sub");
            String email  = jwt.getClaimAsString("email");
            String name   = jwt.getClaimAsString("name");
            Gender gender = personInfo.gender();
            LocalDate birthDate = personInfo.birthDate();

            if (email == null || name == null) {
                throw new IllegalArgumentException("Missing required claims in ID token");
            }

            return new OAuthRegisterDTO(OAuthProvider.GOOGLE, sub, email, name, gender, birthDate);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID token", e);
        }
    }

}
