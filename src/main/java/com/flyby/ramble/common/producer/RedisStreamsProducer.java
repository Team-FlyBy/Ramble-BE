package com.flyby.ramble.common.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisStreamsProducer implements MessageProducer {
    private static final Logger log = LoggerFactory.getLogger(RedisStreamsProducer.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, Object message) {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }

        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        String json;

        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        log.info("Send to Redis topic: {}, message: {}", topic, json);

        redisTemplate.opsForStream().add(MapRecord.create(topic, Map.of("payload", json)));
    }
}
