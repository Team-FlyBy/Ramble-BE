package com.flyby.ramble.auth.handler;

import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Role;
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
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidc)) {
            throw new IllegalArgumentException("인증 주체가 OidcUser 타입이 아닙니다.");
        }
        OidcIdToken idToken = oidc.getIdToken();

        Map<String, Object> claims = new HashMap<>(idToken.getClaims());
        String userId = claims.get("userId").toString();
        String provider = claims.get("provider").toString();

        if (userId == null || provider == null) {
            throw new IllegalArgumentException("userId 또는 provider가 null입니다.");
        }

        String accessToken = jwtUtil.createToken(
                UUID.fromString(userId),
                Role.USER,
                determineDeviceType(request),
                OAuthProvider.from(provider),
                idToken.getSubject());

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private DeviceType determineDeviceType(HttpServletRequest request) {
        String userAgent = Optional.ofNullable(request.getHeader(HttpHeaders.USER_AGENT))
                .orElse("")
                .toLowerCase();

        if (userAgent.contains("android")) {
            return DeviceType.ANDROID;
        }

        if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return DeviceType.IOS;
        }

        return DeviceType.WEB;
    }

}
