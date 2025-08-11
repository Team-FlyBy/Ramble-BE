package com.flyby.ramble.common.config;

import com.flyby.ramble.report.listener.AutoNudeDetectionResultListener;
import com.flyby.ramble.report.listener.NudeDetectionResultListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsConsumerConfig {
    private final RedisConnectionFactory connectionFactory;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();
        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean
    public List<Subscription> streamSubscriptions(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container, AutoNudeDetectionResultListener autoNudeDetectionResultListener, NudeDetectionResultListener nudeDetectionResultListener) {
        try {
            container.start();
            log.info("Redis Stream listener container started successfully");
        } catch (Exception e) {
                log.error("Failed to start Redis Stream listener container", e);
                throw new IllegalStateException("Cannot start Redis Stream listener", e);
        }

        container.start();

        List<Subscription> subscriptions = new ArrayList<>();

        Subscription nudeDetectionSub = container.receive(
                Consumer.from("nude-detection-group", "consumer-nude-detection"),
                StreamOffset.create("nude-detection-result", ReadOffset.lastConsumed()),
                nudeDetectionResultListener::handleMessage
        );

        subscriptions.add(nudeDetectionSub);

        Subscription autoNudeDetectionSub = container.receive(
                Consumer.from("auto-nude-detection-group", "consumer-auto-nude-detection"),
                StreamOffset.create("auto-nude-detection-result", ReadOffset.lastConsumed()),
                autoNudeDetectionResultListener::handleMessage
        );

        subscriptions.add(autoNudeDetectionSub);

        return subscriptions;
    }
}
