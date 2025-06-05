package com.flyby.ramble.common.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "security.http")
public class SecurityHttpProperties {

    @NotEmpty
    private List<String> oAuth2Paths = new ArrayList<>();

    @NotEmpty
    private List<String> permitPaths = new ArrayList<>();

    @NotEmpty
    private Cors cors = new Cors();

    @Setter
    @Getter
    public static class Cors {

        @NotEmpty
        private List<String> allowedOrigins;

        @NotEmpty
        private List<String> allowedMethods;

        @NotEmpty
        private List<String> allowedHeaders;

        private boolean allowCredentials;

        @Positive
        private long maxAge; // Default to 1 hour

    }

    public List<String> getAllowedOrigins() {
        return cors.getAllowedOrigins();
    }

    public List<String> getAllowedMethods() {
        return cors.getAllowedMethods();
    }

    public List<String> getAllowedHeaders() {
        return cors.getAllowedHeaders();
    }

    public boolean isAllowCredentials() {
        return cors.isAllowCredentials();
    }

    public long getMaxAge() {
        return cors.getMaxAge();
    }

}
