package com.flyby.ramble.report.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.report.dto.NudeDetectionResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NudeDetectionResultListener {
    private final ObjectMapper objectMapper;

    public void handleMessage(MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");
            NudeDetectionResultDTO result = objectMapper.readValue(payload, NudeDetectionResultDTO.class);
            log.info("Received nude detection result: {}", result);

        } catch (Exception e) {
            log.error("Failed to process nude detection message", e);
        }
    }
}
