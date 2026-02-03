package com.flyby.ramble.session.repository;

import com.flyby.ramble.session.dto.SessionData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DB 배치 삽입용 세션 데이터
 * SessionBatchRepository 내부에서만 사용
 */
record SessionBatchData(
    UUID externalId,
    LocalDateTime startedAt,
    List<ParticipantBatchData> participants
) {
    public static SessionBatchData from(SessionData sessionData) {
        return new SessionBatchData(
            sessionData.sessionId(),
            sessionData.startedAt(),
            sessionData.participants().stream()
                .map(ParticipantBatchData::from)
                .toList()
        );
    }
}
