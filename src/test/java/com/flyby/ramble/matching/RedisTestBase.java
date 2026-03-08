package com.flyby.ramble.matching;

import com.flyby.ramble.common.config.RedissonConfig;
import com.github.dockerjava.api.model.HostConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(SpringExtension.class)
@TestMethodOrder(OrderAnnotation.class)
@ContextConfiguration(classes = RedissonConfig.class)
public abstract class RedisTestBase {

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379)
            .withCreateContainerCmdModifier(cmd -> {
                HostConfig hostConfig = cmd.getHostConfig();
                if (hostConfig == null) {
                    hostConfig = new HostConfig();
                    cmd.withHostConfig(hostConfig);
                }

                cmd.getHostConfig()
                        .withMemory(512L * 1024 * 1024)     // 메모리 제한: 0.5 GiB = 512 MiB
                        .withMemorySwap(512L * 1024 * 1024) // 스왑 사용 방지
                        .withNanoCPUs(500_000_000L);        // 0.5 vCPU 수준으로 제한
            })
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    protected RedissonClient redissonClient;

    @AfterEach
    void cleanupRedis() {
        redissonClient.getKeys().flushdb();
    }
}
