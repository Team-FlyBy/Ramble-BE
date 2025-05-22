package com.flyby.ramble.auth.filter;

import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> list = List.of(
            "/api-docs/**",
            "/swagger-ui/**",
            "/v3/api-docs/**");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return list.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationToken)) {
            throw new BaseException(ErrorCode.MISSING_ACCESS_TOKEN);
        }

        if (!authorizationToken.startsWith("Bearer ")) {
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        authorizationToken = authorizationToken.substring(7).trim();

        try {
            if (jwtUtil.isExpired(authorizationToken)) {
                throw new BaseException(ErrorCode.EXPIRED_ACCESS_TOKEN);
            }
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
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
