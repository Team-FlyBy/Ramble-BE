package com.flyby.ramble.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.common.model.ResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXCLUDED_URLS = List.of(
            "/api-docs/**",
            "/swagger-ui/**",
            "/v3/api-docs/**");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return EXCLUDED_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationToken)) {
            sendErrorResponse(response, ErrorCode.MISSING_ACCESS_TOKEN);
            return;
        }

        if (!authorizationToken.startsWith("Bearer ")) {
            sendErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        authorizationToken = authorizationToken.substring(7).trim();

        // TODO: UserDetails 구현 필요
        try {
            Claims claims = jwtUtil.parseClaims(authorizationToken);
            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);
            role = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(role)));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException e) {
            log.warn("token expired", e);
            sendErrorResponse(response, ErrorCode.EXPIRED_ACCESS_TOKEN);
            return;
        } catch (Exception e) {
            log.warn("token error", e);
            sendErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ResponseDTO<Object> responseDto = new ResponseDTO<>(errorCode.getHttpStatus().value(), errorCode.getMessage(), Collections.emptyMap());
        String jsonResponse = new ObjectMapper().writeValueAsString(responseDto);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonResponse);
    }
}
