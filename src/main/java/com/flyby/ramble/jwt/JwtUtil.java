package com.flyby.ramble.jwt;

import com.flyby.ramble.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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

    @Value("${jwt.expiration-ms}")
    private long expiration;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            secretKey = Keys.hmacShaKeyFor(secretBytes);
            jwtParser = Jwts.parser().verifyWith(secretKey).build();
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

    public boolean isExpired(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }

    public String createToken(UUID userId, Role role, String tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        // claim이 많아지면 claims로 변경
        // TODO: tenantId 미구현 상태
        return Jwts.builder()
                .claim("role", "ROLE_" + role.name())
                .claim("tenantId", tenantId)
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
}
