package com.flyby.ramble.session.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Redis 세션 데이터
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public record SessionData(
    UUID sessionId,
    LocalDateTime startedAt,
    List<ParticipantData> participants
) {
    public SessionData(LocalDateTime startedAt, List<ParticipantData> participants) {
        this(UUID.randomUUID(), startedAt, participants);
    }
}
