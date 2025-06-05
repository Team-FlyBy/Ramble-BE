package com.flyby.ramble.common.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties 테스트")
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=TestJwtSecretKeyForUnitTestingOnlyMP0EqLD3I=",
        "jwt.issuer=test-issuer",
        "jwt.expiration.access=3600000",
        "jwt.expiration.refresh=604800000"
})
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @DisplayName("jwtProperties 생성 및 값 주입 테스트")
    @Test
    void properties_should_be_bound_correctly() {
        assertThat(jwtProperties).isNotNull();
        assertThat(jwtProperties.getSecret()).isEqualTo("TestJwtSecretKeyForUnitTestingOnlyMP0EqLD3I=");
        assertThat(jwtProperties.getIssuer()).isEqualTo("test-issuer");
        assertThat(jwtProperties.getAccessExpiration()).isEqualTo(3600000L);
        assertThat(jwtProperties.getRefreshExpiration()).isEqualTo(604800000L);
    }

}





