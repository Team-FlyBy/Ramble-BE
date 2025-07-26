package com.flyby.ramble.session.service;

import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;

    public Long getSessionIdBySessionUuid(UUID sessionUuid) {
        Session session = sessionRepository.findByExternalId(sessionUuid).orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));
        return session.getId();
    }
}
