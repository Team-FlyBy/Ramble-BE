package com.flyby.ramble.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

@Component
public class CookieUtil {

    private static final String REFRESH_COOKIE = "refresh";

    @Value("${jwt.expiration-ms.refresh}")
    private long expiration;

    public Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (expiration / 1000));
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    public ResponseCookie createResponseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(expiration / 1000)
                .secure(true)
                .sameSite("Lax")
                .build();
    }

    public Optional<String> getCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, REFRESH_COOKIE);

        return Optional.ofNullable(cookie)
                .map(Cookie::getValue);
    }

}
