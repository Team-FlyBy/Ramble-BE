package com.flyby.ramble.auth.util;

import com.flyby.ramble.model.DeviceType;
import com.flyby.ramble.model.OAuthProvider;
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
        String token = jwtUtil.createToken(uuid, Role.USER, DeviceType.ANDROID, OAuthProvider.GOOGLE, "1223456");
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        System.out.println(token);

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(uuid.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_" + Role.USER.name());
        assertThat(claims.get("deviceType", String.class)).isEqualTo(DeviceType.ANDROID.name());
        assertThat(claims.get("provider", String.class)).isEqualTo(OAuthProvider.GOOGLE.name());
        assertThat(claims.get("providerId", String.class)).isEqualTo("1223456");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(1800000));
    }

    // TODO: 토큰 검증 과정
    //  1. 만료된 토큰 처리
    //  2. 유효하지 않은 서명을 가진 토큰 처리
    //  3. 잘못된 형식의 토큰 처리
    //  4. 필수 클레임이 누락된 토큰 처리

}