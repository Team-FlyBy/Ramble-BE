package com.flyby.ramble.report.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.report.dto.AutoNudeDetectionCompletedEventDTO;
import com.flyby.ramble.report.dto.BanUserByUserUuidCommandDTO;
import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.report.service.UserBanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class AutoNudeDetectionResultListener {
    private final ObjectMapper objectMapper;
    private final UserBanService userBanService;

    public void handleMessage(MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("payload");
            AutoNudeDetectionCompletedEventDTO resultEvent = objectMapper.readValue(payload, AutoNudeDetectionCompletedEventDTO.class);
            log.info("Received nude detection result: {}", resultEvent);

            if (!resultEvent.getIsNude()) {
                return;
            }

            userBanService.banUser(
                    BanUserByUserUuidCommandDTO.builder()
                            .userUuid(resultEvent.getUserUuid())
                            .banReason(BanReason.AUTO_NUDE_DETECTION)
                            .bannedAt(LocalDateTime.now())
                            .build()
            );

        } catch (Exception e) {
            log.error("Failed to process nude detection message", e);
        }
    }
}
