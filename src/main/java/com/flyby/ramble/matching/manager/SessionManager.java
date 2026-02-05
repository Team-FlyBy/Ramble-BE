package com.flyby.ramble.matching.manager;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.util.RedisKeyBuilder;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.event.SessionEndedEvent;
import com.flyby.ramble.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {
    private final RedissonClient redissonClient;
    private final SessionService sessionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매칭 세션 저장
     * DB에 세션 저장하고 Redis에 세션 정보 저장
     */
    public void saveSessions(List<SessionData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // Redis 세션 저장
        saveSessionsToRedis(list);
        // DB 세션 저장 (Spring Retry로 자동 재시도)
        sessionService.saveSessionsAsync(list);
    }

    /**
     * 세션 종료
     * Redis에서 세션 정보 제거하고 SessionEndedEvent 발행
     *
     * @param session 세션 정보
     */
    public void closeSession(SessionData session) {
        if (session == null) {
            return;
        }

        // Redis에서 세션 정보 삭제
        executeDeleteBuckets(buildRelatedKeys(session));
        // 세션 종료 이벤트
        publishEndedEvent(session);

        log.info("매칭 세션 종료: sessionId={}", session.sessionId());
    }

    /**
     * 세션 ID로 세션 조회
     *
     * @param sessionId 세션 ID
     * @return 세션 정보 (없으면 null)
     */
    public SessionData getSession(String sessionId) {
        String sessionKey = RedisKeyBuilder.buildSessionKey(sessionId);

        RBucket<SessionData> bucket = redissonClient.getBucket(sessionKey);
        return bucket.get();
    }

    /**
     * 참가자 ID로 세션 조회
     *
     * @param userId 참가자 ID
     * @return 세션 정보 (없으면 null)
     */
    public SessionData getSessionByUserId(String userId) {
        String sessionUserKey = RedisKeyBuilder.buildSessionUserKey(userId);

        // userId -> sessionId 조회
        RBucket<String> userBucket = redissonClient.getBucket(sessionUserKey);
        String sessionId = userBucket.get();

        if (sessionId == null) {
            return null;
        }

        // sessionId -> SessionData 조회
        return getSession(sessionId);
    }

    /**
     * 사용자의 상대방 ID 조회
     *
     * @param sessionInfo 세션 정보
     * @param userId 사용자 ID
     * @return 상대방 ID
     */
    public String findPartnerId(SessionData sessionInfo, String userId) {
        if (sessionInfo == null) {
            return null;
        }

        for (ParticipantData participant : sessionInfo.participants()) {
            String participantId = participant.userExternalId();
            if (!participantId.equals(userId)) {
                return participantId;
            }
        }

        return null;
    }

    /* --- 내부 메서드 --- */

    private void saveSessionsToRedis(List<SessionData> sessionList) {
        if (sessionList == null || sessionList.isEmpty()) {
            return;
        }

        // 청크 단위로 분할하여 처리
        for (int i = 0; i < sessionList.size(); i += MatchingConstants.REDIS_BATCH_SIZE) {
            List<SessionData> chunk = sessionList.subList(i, Math.min(i + MatchingConstants.REDIS_BATCH_SIZE, sessionList.size()));
            executeSaveBuckets(chunk, Duration.ofMinutes(MatchingConstants.SESSION_TTL));
        }
    }

    private void executeSaveBuckets(List<SessionData> chunk, Duration ttl) {
        RBatch batch = redissonClient.createBatch();

        for (SessionData session : chunk) {
            String sessionId = session.sessionId().toString();

            // 원본 데이터 (sessionID -> SessionDto) 저장
            batch.getBucket(RedisKeyBuilder.buildSessionKey(sessionId)).setAsync(session, ttl);
            // 참조 데이터 (User -> sessionID) 저장
            for (ParticipantData participant : session.participants()) {
                String key = RedisKeyBuilder.buildSessionUserKey(participant.userExternalId());
                batch.getBucket(key).setAsync(sessionId, ttl);
            }
        }

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error("Redis 배치 저장 실패: chunkSize={}", chunk.size(), e);
        }
    }

    /**
     * Bucket 일괄 삭제
     * @param keys 삭제할 bucket key Set
     */
    private void executeDeleteBuckets(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // RBatch를 사용하여 Bucket 여러 개를 한 번에 처리
        RBatch batch = redissonClient.createBatch();

        // 각 Bucket을 제거
        for (String key : keys) {
            batch.getBucket(key).deleteAsync();
        }

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void publishEndedEvent(SessionData sessionInfo) {
        // TODO: Executor 방식도 고려
        CompletableFuture.runAsync(() -> {
            SessionEndedEvent event = SessionEndedEvent.builder()
                    .sessionUuid(sessionInfo.sessionId())
                    .startedAt(sessionInfo.startedAt())
                    .endedAt(LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(event);
        }).exceptionally(ex -> {
            log.error("세션 종료 이벤트 발행 실패: sessionId={}", sessionInfo.sessionId(), ex);
            return null;
        });
    }

    private Set<String> buildRelatedKeys(SessionData sessionInfo) {
        Set<String> keys = new HashSet<>();
        // 원본 키
        String sessionId = sessionInfo.sessionId().toString();
        keys.add(RedisKeyBuilder.buildSessionKey(sessionId));

        // 참조 데이터 키
        for (ParticipantData participant : sessionInfo.participants()) {
            String participantId = participant.userExternalId();
            keys.add(RedisKeyBuilder.buildSessionUserKey(participantId));
        }

        return keys;
    }

}
