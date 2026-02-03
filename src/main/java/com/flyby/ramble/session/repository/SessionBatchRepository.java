package com.flyby.ramble.session.repository;

import com.flyby.ramble.common.util.UuidUtil;
import com.flyby.ramble.session.dto.SessionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionBatchRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;

    public void saveSessionsWithParticipants(List<SessionData> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        // SessionData → SessionBatchData 변환
        List<SessionBatchData> batchDataList = sessions.stream()
                .map(SessionBatchData::from)
                .toList();

        // 리스트를 배치 사이즈로 쪼개서 처리
        for (int i = 0; i < batchDataList.size(); i += BATCH_SIZE) {
            List<SessionBatchData> batchList = batchDataList.subList(i, Math.min(i + BATCH_SIZE, batchDataList.size()));

            try {
                // Session Batch Insert
                insertSessions(batchList);
                // UUID를 키로 session_id 조회
                Map<UUID, Long> sessionIdMap = selectSessionIdMap(batchList);
                // 조회한 ID 매핑
                List<ParticipantBatchData> participants = buildParticipants(batchList, sessionIdMap);

                // SessionParticipant Batch Insert
                if (!participants.isEmpty()) {
                    insertSessionParticipants(participants);
                }
            } catch (Exception e) {
                log.error("Failed to save session batch starting at index {}", i, e);
                throw e;
            }
        }
    }

    private void insertSessions(List<SessionBatchData> sessions) {
        String sql = """
            INSERT INTO sessions (external_id, started_at, created_at, updated_at)
            VALUES (:external_id, :started_at, :created_at, :updated_at)
            """;

        LocalDateTime now = LocalDateTime.now();

        SqlParameterSource[] batch = sessions.stream()
                .map(session -> new MapSqlParameterSource()
                        .addValue("external_id", UuidUtil.uuidToBytes(session.externalId()))
                        .addValue("started_at", session.startedAt())
                        .addValue("created_at", now)
                        .addValue("updated_at", now))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private void insertSessionParticipants(List<ParticipantBatchData> participants) {
        String sql = """
                INSERT INTO session_participants (session_id, user_id, gender, region, language, created_at, updated_at)
                VALUES (:session_id, :user_id, :gender, :region, :language, :created_at, :updated_at)
                """;

        LocalDateTime now = LocalDateTime.now();

        SqlParameterSource[] batch = participants.stream()
                .map(participant -> new MapSqlParameterSource()
                        .addValue("session_id", participant.sessionId())
                        .addValue("user_id", participant.userId())
                        .addValue("gender", participant.gender())
                        .addValue("region", participant.region())
                        .addValue("language", participant.language())
                        .addValue("created_at", now)
                        .addValue("updated_at", now))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private Map<UUID, Long> selectSessionIdMap(List<SessionBatchData> sessions) {
        List<byte[]> externalIds = sessions.stream()
                .map(session -> UuidUtil.uuidToBytes(session.externalId()))
                .toList();

        String sql = """
              SELECT session_id, external_id
              FROM sessions
              WHERE external_id IN (:external_ids)
              """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                sql,
                new MapSqlParameterSource("external_ids", externalIds)
        );

        return result.stream()
                .collect(Collectors.toMap(
                        row -> UuidUtil.bytesToUuid((byte[]) row.get("external_id")),
                        row -> ((Number) row.get("session_id")).longValue()
                ));
    }

    private List<ParticipantBatchData> buildParticipants(List<SessionBatchData> sessions, Map<UUID, Long> sessionIdMap) {
        List<ParticipantBatchData> participants = new ArrayList<>();

        for (SessionBatchData session : sessions) {
            Long sessionId = sessionIdMap.get(session.externalId());

            if (sessionId == null) {
                log.warn("Session not found for UUID: {}", session.externalId());
                continue;
            }

            session.participants().stream()
                    .map(p -> p.withSessionId(sessionId))
                    .forEach(participants::add);
        }

        return participants;
    }

}
