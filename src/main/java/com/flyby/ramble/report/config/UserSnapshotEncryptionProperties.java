package com.flyby.ramble.report.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "encryption.user-snapshot")
public class UserSnapshotEncryptionProperties {
    private String publicKey;
}
