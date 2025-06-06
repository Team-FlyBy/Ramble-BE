package com.flyby.ramble.session.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class SessionEndedEvent {
    private final UUID sessionUuid;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    @Builder
    public SessionEndedEvent(UUID sessionUuid, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.sessionUuid = sessionUuid;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}
