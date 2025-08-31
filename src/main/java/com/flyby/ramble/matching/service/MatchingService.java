package com.flyby.ramble.matching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.dto.MatchingProfileDTO;
import com.flyby.ramble.matching.dto.MatchingSessionInfoDTO;
import com.flyby.ramble.session.event.SessionEndedEvent;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.signaling.dto.SignalMessage;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {
    private static final String SUBSCRIPTION_PREFIX = "/queue/match";
    private static final String SUBSCRIPTION_SUFFIX = "/queue/signal";
    // Redis Keys
    private static final String WAITING_QUEUE_KEY = "webrtc:waiting_queue";
    private static final String USER_PROFILE_KEY  = "webrtc:user_profile";
    private static final String CHAT_SESSION_KEY  = "webrtc:chat_session";
    // Scoring Weights
    private static final int WAITING_SCORE_WEIGHT_PER_10_SECONDS = 1;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final UserService userService;
    private final SessionService sessionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 신규 사용자의 매칭을 요청하거나 대기열에 추가
     */
    public void findMatchOrAddToQueue(String requesterId, String requesterRegion, MatchRequestDTO request) {
        cleanupUser(requesterId);

        UserInfoDTO requesterInfo = userService.getUserByExternalId(requesterId);

        MatchingProfileDTO requesterProfile = MatchingProfileDTO.builder()
                .id(requesterInfo.getId())
                .externalId(requesterId)
                .region(requesterRegion)
                .gender(requesterInfo.getGender())
                .language(request.language())
                .selectedRegion(request.region())
                .selectedGender(request.gender())
                .build();

        redisTemplate.opsForHash().put(USER_PROFILE_KEY, requesterId, requesterProfile);

        findBestPartner(requesterProfile).ifPresentOrElse(
                partner -> handleMatchSuccess(requesterProfile, partner),
                () -> addToWaitingQueue(requesterProfile)
        );
    }

    /**
     * WebRTC 시그널링 메시지를 상대방에게 중계
     * @param senderId 메시지를 보낸 사용자
     * @param message    전송할 SignalMessage
     */
    public void forwardSignalingMessage(String senderId, SignalMessage message) {
        messagingTemplate.convertAndSendToUser(message.getReceiverId(), SUBSCRIPTION_SUFFIX, message);
    }

    /**
     * 사용자가 연결을 끊었을 때 정리
     */
    public void cleanupUser(String userId) {
        cancelWaitingUser(userId); // 대기열에 있을 때 -> 대기열 및 프로필에서 제거
        leaveChatSession(userId);  // 채팅 세션에서 제거 및 상대방에게 알림
    }

    private void cancelWaitingUser(String userId) {
        Long removedCount = redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, userId);

        if (removedCount != null && removedCount > 0) {
            redisTemplate.opsForHash().delete(USER_PROFILE_KEY, userId);
        }
    }

    private void leaveChatSession(String userId) {
        Object object = redisTemplate.opsForHash().get(CHAT_SESSION_KEY, userId);

        if (object != null) {
            MatchingSessionInfoDTO sessionInfo = objectMapper.convertValue(object, MatchingSessionInfoDTO.class);

            String partnerId = sessionInfo.getPartnerInfo();
            MatchResultDTO payload = new MatchResultDTO("LEAVE", userId);

            redisTemplate.opsForHash().delete(CHAT_SESSION_KEY, userId, partnerId);
            messagingTemplate.convertAndSendToUser(partnerId, SUBSCRIPTION_PREFIX, payload);

            // 세션 종료 이벤트 발행
            SessionEndedEvent sessionEndedEvent = SessionEndedEvent.builder()
                    .sessionUuid(sessionInfo.getSessionId())
                    .startedAt(sessionInfo.getStartedAt())
                    .endedAt(java.time.LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(sessionEndedEvent);
        }
    }

    /**
     * 대기열을 스캔하여 가장 적합한 파트너 탐색
     * @param requester 요청자 정보
     * @return 찾은 파트너 정보 (Optional)
     */
    private Optional<MatchingProfileDTO> findBestPartner(MatchingProfileDTO requester) {
        List<Object> waitingUserIds = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);

        if (waitingUserIds == null || waitingUserIds.isEmpty()) {
            return Optional.empty();
        }

        MatchingProfileDTO bestMatch = null;
        int maxScore = -1;

        // 대기열 순회 (FIFO 우선)
        for (Object partnerIdObj : waitingUserIds) {
            String partnerId = (String) partnerIdObj;
            Object object = redisTemplate.opsForHash().get(USER_PROFILE_KEY, partnerId);

            if (object == null) continue;

            MatchingProfileDTO partner = objectMapper.convertValue(object, MatchingProfileDTO.class);

            if (!isMatchable(requester, partner)) continue;

            int currentScore = calculateScore(requester, partner);

            if (currentScore > maxScore) {
                maxScore = currentScore;
                bestMatch = partner;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    private boolean isMatchable(MatchingProfileDTO userA, MatchingProfileDTO userB) {
        // 1. 자기 자신과 매칭 불가
        if (Objects.equals(userA.getId(), userB.getId())) return false;

        // 2. 상호 차단 목록 확인 (서로 신고 이력이 있으면 매칭 불가)
        // TODO: 차단 목록 로직 구현

        return true;
    }

    /**
     * 두 사용자 간의 매칭 점수 계산
     */
    private int calculateScore(MatchingProfileDTO userA, MatchingProfileDTO userB) {
        int score = 0;

        // 지역 일치 시 점수 추가
        if (Objects.equals(userA.getSelectedRegion(), userB.getRegion())) score += 15;
        if (Objects.equals(userA.getRegion(), userB.getSelectedRegion())) score += 15;
        if (Objects.equals(userA.getRegion(), userB.getRegion())) score += 5;

        // 성별 다를 시 점수 추가
        if (!Objects.equals(userA.getSelectedGender(), userB.getSelectedGender())) score += 5;
        if (!Objects.equals(userA.getSelectedGender(), userB.getGender())) score += 5;
        if (!Objects.equals(userA.getGender(), userB.getSelectedGender())) score += 5;
        if (!Objects.equals(userA.getGender(), userB.getGender())) score += 5;

        // 언어 일치 시 점수 추가
        if (Objects.equals(userA.getLanguage(), userB.getLanguage())) score += 5;

        // 대기 시간에 비례하여 점수 추가 (10초당 1점)
        long currentTime = System.currentTimeMillis();
        long waitTimeB = (currentTime - userB.getQueueEntryTime()) / 1000;
        score += (int) (waitTimeB / 10) * WAITING_SCORE_WEIGHT_PER_10_SECONDS;

        return score;
    }

    private void handleMatchSuccess(MatchingProfileDTO userA, MatchingProfileDTO userB) {
        log.info("매칭 성공: {} <-> {}", userA.getExternalId(), userB.getExternalId());

        redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, userB.getId());

        // session 저장
        Session session = sessionService.createSession(userA, userB);

        // Redis에 채팅 세션 기록
        MatchingSessionInfoDTO sessionInfoForA = new MatchingSessionInfoDTO(session.getExternalId(), userB.getExternalId(), session.getStartedAt());
        MatchingSessionInfoDTO sessionInfoForB = new MatchingSessionInfoDTO(session.getExternalId(), userA.getExternalId(), session.getStartedAt());
        redisTemplate.opsForHash().put(CHAT_SESSION_KEY, userA.getExternalId(), sessionInfoForA);
        redisTemplate.opsForHash().put(CHAT_SESSION_KEY, userB.getExternalId(), sessionInfoForB);

        // 양쪽 사용자에게 매칭 성공 알림 전송
        MatchResultDTO resultForA = new MatchResultDTO("SUCCESS", userB.getExternalId());
        MatchResultDTO resultForB = new MatchResultDTO("SUCCESS", userA.getExternalId());
        messagingTemplate.convertAndSendToUser(userA.getExternalId(), SUBSCRIPTION_PREFIX, resultForA);
        messagingTemplate.convertAndSendToUser(userB.getExternalId(), SUBSCRIPTION_PREFIX, resultForB);
    }

    private void addToWaitingQueue(MatchingProfileDTO userProfile) {
        log.info("대기열에 추가: {}", userProfile.getExternalId());

        String userId = userProfile.getExternalId();
        userProfile.setQueueEntryTime(System.currentTimeMillis());

        redisTemplate.opsForHash().put(USER_PROFILE_KEY, userId, userProfile);
        redisTemplate.opsForList().rightPush(WAITING_QUEUE_KEY, userId);

        MatchResultDTO waitingResult = new MatchResultDTO("WAITING", null);
        messagingTemplate.convertAndSendToUser(userId, SUBSCRIPTION_PREFIX, waitingResult);
    }

}
