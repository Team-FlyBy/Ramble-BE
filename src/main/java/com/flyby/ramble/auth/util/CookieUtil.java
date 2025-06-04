package com.flyby.ramble.auth.util;

import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.properties.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtProperties jwtProperties;

    public Cookie createCookie(String value) {
        Cookie cookie = new Cookie(JwtConstants.REFRESH_COOKIE, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (jwtProperties.getRefreshExpiration() / 1000));
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    public ResponseCookie createResponseCookie(String value) {
        return ResponseCookie.from(JwtConstants.REFRESH_COOKIE, value)
                .path("/")
                .httpOnly(true)
                .maxAge(jwtProperties.getRefreshExpiration() / 1000)
                .secure(true)
                .sameSite("Lax")
                .build();
    }

    public Optional<String> getCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, JwtConstants.REFRESH_COOKIE);

        return Optional.ofNullable(cookie)
                .map(Cookie::getValue);
    }

}
