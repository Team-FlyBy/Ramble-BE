package com.flyby.ramble.session.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 세션 종료 이벤트
 */
@Getter
@ToString
public class SessionEndedEvent {
    private final UUID sessionUuid;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    @Builder
    public SessionEndedEvent(UUID sessionUuid, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (sessionUuid == null) {
            throw new IllegalArgumentException("UUID must not be null");
        }
        if (startedAt == null || endedAt == null) {
            throw new IllegalArgumentException("Both startedAt and endedAt must not be null");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must be after startedAt");
        }

        this.sessionUuid = sessionUuid;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}
