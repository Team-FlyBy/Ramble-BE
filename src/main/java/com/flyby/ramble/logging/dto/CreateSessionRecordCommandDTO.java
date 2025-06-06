package com.flyby.ramble.logging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CreateSessionRecordCommandDTO {
    private final UUID sessionUuid;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    @Builder
    public CreateSessionRecordCommandDTO(UUID sessionUuid, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.sessionUuid = sessionUuid;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}
