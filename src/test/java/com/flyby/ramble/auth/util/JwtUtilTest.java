package com.flyby.ramble.auth.util;

import com.flyby.ramble.auth.dto.JwtTokenRequest;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.properties.JwtProperties;
import com.flyby.ramble.user.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DisplayName("JwtUtil 테스트")
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtUtil jwtUtil;

    JwtTokenRequest request;

    @BeforeEach
    void setUp() {
        request = new JwtTokenRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Role.ROLE_USER,
                DeviceType.ANDROID,
                OAuthProvider.GOOGLE,
                "1223456"
        );

        given(jwtProperties.getSecret()).willReturn("TestJwtSecretKeyForUnitTestingOnlyMP0EqLD3I=");
        given(jwtProperties.getIssuer()).willReturn("test-issuer");
    }

    @DisplayName("Access 생성 테스트")
    @Test
    void createToken() {
        given(jwtProperties.getAccessExpiration()).willReturn(3600000L);
        jwtUtil.init();

        String token = jwtUtil.generateAccToken(request);
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getId()).isEqualTo(request.jti().toString());
        assertThat(claims.getSubject()).isEqualTo(request.userExternalId().toString());

        assertThat(claims.get(JwtConstants.CLAIM_AUTHORITIES, String.class)).isEqualTo(request.role().name());
        assertThat(claims.get(JwtConstants.CLAIM_TOKEN_TYPE,  String.class)).isEqualTo(JwtConstants.TOKEN_TYPE_ACCESS);
        assertThat(claims.get(JwtConstants.CLAIM_DEVICE_TYPE, String.class)).isEqualTo(request.deviceType().name());
        assertThat(claims.get(JwtConstants.CLAIM_PROVIDER,    String.class)).isEqualTo(request.provider().name());
        assertThat(claims.get(JwtConstants.CLAIM_PROVIDER_ID, String.class)).isEqualTo(request.providerId());

        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(3600000));
    }
//
    @DisplayName("Refresh 생성 테스트")
    @Test
    void createRefreshToken() {
        given(jwtProperties.getRefreshExpiration()).willReturn(604800000L);
        jwtUtil.init();

        String token = jwtUtil.generateRefToken(request);
        assertThat(token).isNotNull();

        Instant now = Instant.now();

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getId()).isEqualTo(request.jti().toString());
        assertThat(claims.getSubject()).isEqualTo(request.userExternalId().toString());

        assertThat(claims.get(JwtConstants.CLAIM_AUTHORITIES, String.class)).isEqualTo(request.role().name());
        assertThat(claims.get(JwtConstants.CLAIM_TOKEN_TYPE,  String.class)).isEqualTo(JwtConstants.TOKEN_TYPE_REFRESH);
        assertThat(claims.get(JwtConstants.CLAIM_DEVICE_TYPE, String.class)).isEqualTo(request.deviceType().name());
        assertThat(claims.get(JwtConstants.CLAIM_PROVIDER,    String.class)).isEqualTo(request.provider().name());
        assertThat(claims.get(JwtConstants.CLAIM_PROVIDER_ID, String.class)).isEqualTo(request.providerId());

        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getExpiration()).isAfter(now);
        assertThat(claims.getExpiration()).isBefore(now.plusMillis(604800000));
    }

    @DisplayName("만료된 토큰 테스트")
    @Test
    void expiredTokenTest() {
        jwtUtil.init();

        String expiredToken = ReflectionTestUtils.invokeMethod(jwtUtil, "createToken",
                request, JwtConstants.TOKEN_TYPE_ACCESS, 0L);

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
        given(jwtProperties.getAccessExpiration()).willReturn(3600000L);
        jwtUtil.init();

        String token = jwtUtil.generateAccToken(request);
        String invalidToken = token.substring(0, token.lastIndexOf(".") + 1) + "invalidSignature";

        try {
            Claims claims = jwtUtil.parseClaims(invalidToken);
            assertThat(claims).isNull(); // 예외가 발생하지 않으면 실패하는 테스트
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SignatureException.class);
        }
    }

}