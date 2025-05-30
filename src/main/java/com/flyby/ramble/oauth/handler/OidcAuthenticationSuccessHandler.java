package com.flyby.ramble.oauth.handler;

import com.flyby.ramble.oauth.model.CustomOidcUser;
import com.flyby.ramble.auth.service.JwtService;
import com.flyby.ramble.user.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof CustomOidcUser oidcUser)) {
            throw new IllegalArgumentException("인증 주체가 OidcUser 타입이 아닙니다.");
        }

        User user = oidcUser.getUser();

        jwtService.generateTokens(user, response);
    }

}
