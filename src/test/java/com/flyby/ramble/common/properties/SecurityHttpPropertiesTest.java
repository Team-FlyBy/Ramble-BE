package com.flyby.ramble.common.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("SecurityHttpProperties 테스트")
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(SecurityHttpProperties.class)
@TestPropertySource(properties = {
        "security.http.oauth2-paths[0]=/oauth2/test",
        "security.http.oauth2-paths[1]=/oauth2/test2",
        "security.http.permit-paths[0]=/public",
        "security.http.permit-paths[1]=/health",
        "security.http.cors.allowed-origins[0]=example.com",
        "security.http.cors.allowed-methods[0]=GET",
        "security.http.cors.allowed-headers[0]=Authorization",
        "security.http.cors.allowed-headers[1]=Content-Type",
        "security.http.cors.allow-credentials=true",
        "security.http.cors.max-age=3600"
})
class SecurityHttpPropertiesTest {

    @Autowired
    private SecurityHttpProperties securityHttpProperties;

    @DisplayName("SecurityHttpProperties 생성 및 값 주입 테스트")
    @Test
    void properties_should_be_bound_correctly() {
        assertThat(securityHttpProperties).isNotNull();
        assertThat(securityHttpProperties.getOAuth2Paths()).isEqualTo(List.of("/oauth2/test", "/oauth2/test2"));
        assertThat(securityHttpProperties.getPermitPaths()).isEqualTo(List.of("/public", "/health"));
        assertThat(securityHttpProperties.getAllowedOrigins()).isEqualTo(List.of("example.com"));
        assertThat(securityHttpProperties.getAllowedMethods()).isEqualTo(List.of("GET"));
        assertThat(securityHttpProperties.getAllowedHeaders()).isEqualTo(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        assertThat(securityHttpProperties.isAllowCredentials()).isTrue();
        assertThat(securityHttpProperties.getMaxAge()).isEqualTo(3600L); // 1 hour in seconds

    }

}
