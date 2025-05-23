package com.flyby.ramble.auth.util;

import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.issuer=test-issuer",
        "jwt.expiration-ms.access=3600000",   // 1시간
        "jwt.expiration-ms.refresh=604800000" // 7일
})
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    String userId;
    Role role;
    DeviceType deviceType;
    OAuthProvider provider;
    String providerId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        role = Role.USER;
        deviceType = DeviceType.ANDROID;
        provider = OAuthProvider.GOOGLE;
        providerId = "1223456";
    }

    @DisplayName("Access 생성 테스트")
    @Test
    void createToken() {
        String token = jwtUtil.generateAccToken(userId, role, deviceType, provider, providerId);
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("role",       String.class)).isEqualTo("ROLE_" + role.name());
        assertThat(claims.get("type",       String.class)).isEqualTo("access");
        assertThat(claims.get("deviceType", String.class)).isEqualTo(deviceType.name());
        assertThat(claims.get("provider",   String.class)).isEqualTo(provider.name());
        assertThat(claims.get("providerId", String.class)).isEqualTo(providerId);
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(3600000));
    }

    @DisplayName("Refresh 생성 테스트")
    @Test
    void createRefreshToken() {
        String token = jwtUtil.generateRefToken(userId, role, deviceType, provider, providerId);
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("role",       String.class)).isEqualTo("ROLE_" + role.name());
        assertThat(claims.get("type",       String.class)).isEqualTo("refresh");
        assertThat(claims.get("deviceType", String.class)).isEqualTo(deviceType.name());
        assertThat(claims.get("provider",   String.class)).isEqualTo(provider.name());
        assertThat(claims.get("providerId", String.class)).isEqualTo(providerId);
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(604800000));
    }

    @DisplayName("만료된 토큰 테스트")
    @Test
    void expiredTokenTest() {
        String expiredToken = ReflectionTestUtils.invokeMethod(jwtUtil, "createToken",
                userId, role, "access", deviceType, provider, providerId, 0L);

        try {
            Claims claims = jwtUtil.parseClaims(expiredToken);
            assertThat(claims).isNull(); // 예외가 발생하지 않으면 실패하는 테스트
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ExpiredJwtException.class);
        }
    }

    @DisplayName("잘못된 토큰 테스트")
    @Test
    void invalidSignatureTest() {
        String token = jwtUtil.generateAccToken(userId, role, deviceType, provider, providerId);
        String invalidToken = token.substring(0, token.lastIndexOf(".") + 1) + "invalidSignature";

        try {
            Claims claims = jwtUtil.parseClaims(invalidToken);
            assertThat(claims).isNull(); // 예외가 발생하지 않으면 실패하는 테스트
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SignatureException.class);
        }
    }

}