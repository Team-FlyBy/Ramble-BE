package com.flyby.ramble.logging.listener;

import com.flyby.ramble.logging.model.meta.SessionRecord;
import com.flyby.ramble.logging.repository.meta.SessionRecordRepository;
import com.flyby.ramble.session.event.SessionEndedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SessionRecordListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SessionRecordRepository sessionRecordRepository;

    @Test
    @DisplayName("세션 종료 후 세션 레코드 데이터를 생성한다.")
    void handle() {
        // given
        UUID sessionUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        SessionEndedEvent sessionEndedEvent = SessionEndedEvent.builder()
                .sessionUuid(sessionUuid)
                .startedAt(now)
                .endedAt(now.plusHours(1))
                .build();

        // when
        eventPublisher.publishEvent(sessionEndedEvent);

        // then
        SessionRecord sessionRecord = sessionRecordRepository.findByUuid(sessionUuid).get();

        assertThat(sessionRecord).isNotNull();
    }
}