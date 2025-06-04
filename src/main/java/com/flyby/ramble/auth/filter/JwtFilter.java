package com.flyby.ramble.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.auth.service.AuthService;
import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.common.dto.ResponseDTO;
import com.flyby.ramble.common.properties.SecurityHttpProperties;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final SecurityHttpProperties securityHttpProperties;

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return securityHttpProperties.getPermitPaths()
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!isValidToken(authHeader, response)) {
            return;
        }

        try {
            Authentication auth = jwtUtil.parseAuthentication(authHeader.substring(7).trim());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, ErrorCode.EXPIRED_ACCESS_TOKEN);
            return;
        } catch (Exception e) {
            sendErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String authHeader, HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(authHeader)) {
            sendErrorResponse(response, ErrorCode.MISSING_ACCESS_TOKEN);
            return false;
        }

        if (!authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return false;
        }

        if (authService.isBlacklisted(authHeader.substring(7).trim())) {
            sendErrorResponse(response, ErrorCode.BLOCKED_ACCESS_TOKEN);
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        log.warn(errorCode.getMessage());

        ResponseDTO<Object> responseDto = new ResponseDTO<>(errorCode.getHttpStatus().value(), errorCode.getMessage(), Collections.emptyMap());
        String jsonResponse = new ObjectMapper().writeValueAsString(responseDto);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonResponse);
    }
}
