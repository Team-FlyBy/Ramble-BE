package com.flyby.ramble.common.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer;

    private Expiration expiration = new Expiration();

    @Setter
    @Getter
    public static class Expiration {

        @Positive
        private long access;

        @Positive
        private long refresh;

    }

    public long getAccessExpiration() {
        return expiration.access;
    }

    public long getRefreshExpiration() {
        return expiration.refresh;
    }

}
