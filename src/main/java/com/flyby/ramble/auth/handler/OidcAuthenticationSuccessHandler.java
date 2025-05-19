package com.flyby.ramble.auth.handler;

import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.model.DeviceType;
import com.flyby.ramble.model.OAuthProvider;
import com.flyby.ramble.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        OidcIdToken idToken = oidc.getIdToken();

        Map<String, Object> claims = new HashMap<>(idToken.getClaims());
        String userId = claims.get("userId").toString();
        String provider = claims.get("provider").toString();
        String providerId = idToken.getSubject();

        String accessToken = jwtUtil.createToken(
                UUID.fromString(userId),
                Role.USER,
                DeviceType.WEB,
                OAuthProvider.valueOf(provider),
                providerId);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
