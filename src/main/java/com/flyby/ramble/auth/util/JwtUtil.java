package com.flyby.ramble.auth.util;

import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-ms.access}")
    private long accessExpiration;

    @Value("${jwt.expiration-ms.refresh}")
    private long refreshExpiration;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
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

    public String generateAccToken(String userId, Role role, DeviceType deviceType, OAuthProvider provider, String providerId) {
        return createToken(userId, role, "access", deviceType, provider, providerId, accessExpiration);
    }

    public String generateRefToken(String userId, Role role, DeviceType deviceType, OAuthProvider provider, String providerId) {
        return createToken(userId, role, "refresh", deviceType, provider, providerId, refreshExpiration);
    }

    private String createToken(@NonNull String userId, @NonNull Role role, @NonNull String tokenType, @NonNull DeviceType deviceType, @NonNull OAuthProvider provider, @NonNull String providerId, long expiration) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        Map<String, Object> claims = Map.of(
                "jti",  UUID.randomUUID().toString(),
                "role","ROLE_" + role.name(),
                "type", tokenType,
                "deviceType", deviceType.name(),
                "provider",   provider.name(),
                "providerId", providerId
        );

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
}
