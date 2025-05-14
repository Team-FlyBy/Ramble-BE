package com.flyby.ramble.jwt;

import com.flyby.ramble.model.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.issuer=test-issuer",
        "jwt.expiration-ms=1800000"  // 30분
})
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @DisplayName("JWT 생성 테스트")
    @Test
    void createToken() {
        UUID uuid = UUID.randomUUID();
        String token = jwtUtil.createToken(uuid, Role.USER, "testTenantId");
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(uuid.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.name());
        assertThat(claims.get("tenantId", String.class)).isEqualTo("testTenantId");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(1800000));
    }

}