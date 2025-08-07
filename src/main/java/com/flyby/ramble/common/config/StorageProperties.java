package com.flyby.ramble.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private final String endpoint;
    private final String bucket;
    private final Credentials credentials;

    @Getter
    @RequiredArgsConstructor
    public static class Credentials {
        private final String accessKey;
        private final String secretKey;
    }
}
