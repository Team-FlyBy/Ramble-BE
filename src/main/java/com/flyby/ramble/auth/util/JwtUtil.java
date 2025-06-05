package com.flyby.ramble.auth.util;

import com.flyby.ramble.auth.dto.JwtTokenRequest;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
            secretKey = Keys.hmacShaKeyFor(secretBytes);
            jwtParser = Jwts.parser().verifyWith(secretKey).build();
        } catch (WeakKeyException e) {
            log.error("secret<ERROR>: HMAC 키 길이가 너무 짧습니다(32byte 이상 필요).", e);
            throw new IllegalStateException("JWT 시크릿 키가 취약합니다(길이 부족)", e);
        } catch (IllegalArgumentException e) {
            log.error("secret<ERROR>: 유효하지 않은 Base64 인코딩", e);
            throw new IllegalStateException("JWT 시크릿 키 디코딩 실패", e);
        } catch (Exception e) {
            log.error("secret<ERROR>: 시크릿 키 초기화 중 오류", e);
            throw new IllegalStateException("JWT 시크릿 키 초기화 실패", e);
        }
    }

    public Claims parseClaims(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    public Authentication parseAuthentication(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.getSubject();
        String role   = claims.get(JwtConstants.CLAIM_AUTHORITIES, String.class);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        UserDetails principal = User.builder()
                .username(userId)
                .password("")
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    public String generateAccToken(JwtTokenRequest request) {
        return createToken(request, JwtConstants.TOKEN_TYPE_ACCESS,  jwtProperties.getAccessExpiration());
    }

    public String generateRefToken(JwtTokenRequest request) {
        return createToken(request, JwtConstants.TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpiration());
    }

    private String createToken(@NonNull JwtTokenRequest request, @NonNull String tokenType, long expiration) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        Map<String, Object> claims = Map.of(
                JwtConstants.CLAIM_AUTHORITIES, request.role().name(),
                JwtConstants.CLAIM_TOKEN_TYPE,  tokenType,
                JwtConstants.CLAIM_DEVICE_TYPE, request.deviceType().name(),
                JwtConstants.CLAIM_PROVIDER,    request.provider().name(),
                JwtConstants.CLAIM_PROVIDER_ID, request.providerId()
        );

        return Jwts.builder()
                .id(request.jti().toString())
                .claims(claims)
                .subject(request.userExternalId().toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
}
