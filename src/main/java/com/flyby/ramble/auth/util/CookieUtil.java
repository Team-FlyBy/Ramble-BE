package com.flyby.ramble.auth.util;

import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.properties.JwtProperties;
import com.flyby.ramble.common.properties.SecurityHttpProperties;
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
    private final SecurityHttpProperties securityHttpProperties;

    public Cookie createCookie(String value) {
        Cookie cookie = new Cookie(JwtConstants.REFRESH_COOKIE, value);
        cookie.setPath("/");
        cookie.setDomain("." + securityHttpProperties.getDomain());
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (jwtProperties.getRefreshExpiration() / 1000));
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    public ResponseCookie createResponseCookie(String value) {
        return ResponseCookie.from(JwtConstants.REFRESH_COOKIE, value)
                .path("/")
                .domain("." + securityHttpProperties.getDomain())
                .httpOnly(true)
                .maxAge(jwtProperties.getRefreshExpiration() / 1000)
                .secure(true)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie deleteResponseCookie() {
        return ResponseCookie.from(JwtConstants.REFRESH_COOKIE, "")
                .path("/")
                .domain("." + securityHttpProperties.getDomain())
                .httpOnly(true)
                .maxAge(0) // 쿠키 삭제를 위해 maxAge를 0으로 설정
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
