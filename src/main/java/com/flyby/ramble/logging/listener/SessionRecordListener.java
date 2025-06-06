package com.flyby.ramble.logging.listener;

import com.flyby.ramble.logging.dto.CreateSessionRecordCommandDTO;
import com.flyby.ramble.logging.service.SessionRecordService;
import com.flyby.ramble.session.event.SessionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 세션 레코드 이벤트 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionRecordListener {
    private final SessionRecordService sessionRecordService;

    @EventListener
    @Async
    public void handle(SessionEndedEvent event) {
        log.debug("received sessionEndedEvent: {}", event);
        try {
            sessionRecordService.createSessionRecord(
                    CreateSessionRecordCommandDTO.builder()
                            .sessionUuid(event.getSessionUuid())
                            .startedAt(event.getStartedAt())
                            .endedAt(event.getEndedAt())
                            .build()
            );
        } catch (Exception e) {
            log.error("세션 레코드 생성 중 에러 발생: sessionUuid={}", event.getSessionUuid(), e);
            // 필요시 재시도 로직이나 DLQ 처리 추가
        }
    }
}
