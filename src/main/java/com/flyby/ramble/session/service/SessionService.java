package com.flyby.ramble.session.service;

import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionBatchRepository;
import com.flyby.ramble.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SessionBatchRepository sessionBatchRepository;

    public Long getSessionIdBySessionUuid(UUID sessionUuid) {
        Session session = sessionRepository.findByExternalId(sessionUuid).orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));
        return session.getId();
    }

    public void saveSessions(List<SessionData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        sessionBatchRepository.saveSessionsWithParticipants(list);
    }

    // TODO: @Retryable이 동작하지 않을 수 있음. 추후 수정

    @Async
    @Retryable(
            retryFor = {DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> saveSessionsAsync(List<SessionData> list) {
        if (list == null || list.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        sessionBatchRepository.saveSessionsWithParticipants(list);
        log.debug("세션 저장 완료: {} 건", list.size());
        return CompletableFuture.completedFuture(null);
    }

    @Recover
    public CompletableFuture<Void> recoverSaveSessionsAsync(DataAccessException e, List<SessionData> list) {
        log.error("세션 저장 최종 실패 (재시도 소진): {} 건", list.size(), e);
        // TODO: 추후 실패 처리 로직 추가
        return CompletableFuture.completedFuture(null);
    }

}