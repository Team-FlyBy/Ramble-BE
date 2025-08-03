package com.flyby.ramble.common.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisStreamProducer implements MessageProducer {
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

        redisTemplate.opsForStream().add(MapRecord.create(topic, Map.of("payload", json)));
    }
}
