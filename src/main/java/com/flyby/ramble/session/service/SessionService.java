package com.flyby.ramble.session.service;

import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionBatchRepository;
import com.flyby.ramble.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> saveSessionsAsync(List<SessionData> list) {
        if (list == null || list.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            sessionBatchRepository.saveSessionsWithParticipants(list);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 세션 저장 실패: {} 건", list.size(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

}