package com.flyby.ramble.session.service;

import com.flyby.ramble.matching.model.MatchingProfile;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserService userService;

    public Long getSessionIdBySessionUuid(UUID sessionUuid) {
        Session session = sessionRepository.findByExternalId(sessionUuid).orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));
        return session.getId();
    }

    public Session createSession(MatchingProfile userA, MatchingProfile userB) {
        if (userA == null || userB == null) {
            throw new IllegalArgumentException("매칭 프로필이 누락되었습니다.");
        }

        if (userA.getUserId().equals(userB.getUserId())) {
            throw new IllegalArgumentException("같은 사용자끼리는 세션을 생성할 수 없습니다.");
        }

        User userAEntity = userService.getUserProxyById(userA.getUserId());
        User userBEntity = userService.getUserProxyById(userB.getUserId());

        Session session = Session.builder()
                .startedAt(LocalDateTime.now())
                .build();

        session.addParticipant(userAEntity, userA.getGender(), userA.getRegion(), userA.getLanguage());
        session.addParticipant(userBEntity, userB.getGender(), userB.getRegion(), userB.getLanguage());

        return sessionRepository.save(session);
    }

}
