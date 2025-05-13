package com.flyby.ramble.jwt;

import com.flyby.ramble.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-ms}")
    private long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createToken(UUID userId, Role role, String tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        // claim이 많아지면 claims로 변경
        // TODO: tenantId 미구현 상태
        return Jwts.builder()
                .claim("role", role)
                .claim("tenantId", tenantId)
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
}
