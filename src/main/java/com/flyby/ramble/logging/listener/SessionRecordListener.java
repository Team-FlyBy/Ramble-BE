package com.flyby.ramble.logging.listener;

import com.flyby.ramble.logging.dto.CreateSessionRecordCommandDTO;
import com.flyby.ramble.logging.service.SessionRecordService;
import com.flyby.ramble.session.event.SessionEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionRecordListener {

    private final SessionRecordService sessionRecordService;

    @EventListener
    public void handle(SessionEndedEvent event) {
        sessionRecordService.createSessionRecord(
                CreateSessionRecordCommandDTO.builder()
                        .sessionUuid(event.getSessionUuid())
                        .startedAt(event.getStartedAt())
                        .endedAt(event.getEndedAt())
                        .build()
        );
    }
}
