package com.flyby.ramble.report.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.report.dto.NudeDetectionCompletedEventDTO;
import com.flyby.ramble.report.service.UserReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NudeDetectionResultListener {
    private final ObjectMapper objectMapper;
    private final UserReportService userReportService;

    public void handleMessage(MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");

            if (payload == null) {
                log.error("Received message with null payload");
                return;
            }

            NudeDetectionCompletedEventDTO result = objectMapper.readValue(payload, NudeDetectionCompletedEventDTO.class);
            log.info("Received nude detection result: {}", result);

            if (!result.getIsNude()) {
                return;
            }

            userReportService.banUserByNudeDetection(result.getReportUuid());

        } catch (Exception e) {
            log.error("Failed to process nude detection message", e);
        }
    }
}
