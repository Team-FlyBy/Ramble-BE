package com.flyby.ramble.auth.filter;

import com.flyby.ramble.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizationToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationToken) || !authorizationToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        authorizationToken = authorizationToken.substring(7).trim();

        // TODO: 토큰 예외 처리 보완 필요
        try {
            if (jwtUtil.isExpired(authorizationToken)) {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // TODO: UserDetails 구현 필요
        Claims claims = jwtUtil.parseClaims(authorizationToken);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        role = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(role)));

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
