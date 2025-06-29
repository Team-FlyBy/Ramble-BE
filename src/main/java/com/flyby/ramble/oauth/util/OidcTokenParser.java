package com.flyby.ramble.oauth.util;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.oauth.dto.OAuthRegisterDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Slf4j
@UtilityClass
public class OidcTokenParser {

    public OAuthRegisterDTO parseGoogleIdToken(String idToken) {
        try {
            JwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .build();

            Jwt jwt = jwtDecoder.decode(idToken);
            String email = jwt.getClaimAsString("email");
            String name  = jwt.getClaimAsString("name");
            String sub   = jwt.getClaimAsString("sub"); // 구글 사용자 ID

            return new OAuthRegisterDTO(email, name, OAuthProvider.GOOGLE, sub);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID token", e);
        }
    }

}
