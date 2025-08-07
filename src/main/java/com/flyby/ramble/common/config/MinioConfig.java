package com.flyby.ramble.common.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {
    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getCredentials().getAccessKey(), storageProperties.getCredentials().getSecretKey())
                .build();
    }

    public String getBucket() {
        return storageProperties.getBucket();
    }
}
