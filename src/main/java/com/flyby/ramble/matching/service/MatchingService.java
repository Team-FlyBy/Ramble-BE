package com.flyby.ramble.matching.service;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.dto.MatchingProfileDTO;
import com.flyby.ramble.matching.dto.MatchingSessionInfoDTO;
import com.flyby.ramble.session.event.SessionEndedEvent;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.matching.dto.SignalMessageDTO;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;

    private final UserService userService;
    private final SessionService sessionService;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 신규 사용자의 매칭을 요청하거나 대기열에 추가
     */
    public void findMatchOrAddToQueue(String requesterId, String requesterRegion, MatchRequestDTO request) {
        RLock lock = redissonClient.getLock(MatchingConstants.MATCHMAKING_LOCK_KEY);

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("매칭 락 획득 실패: {}", requesterId);
                throw new IllegalStateException("일시적으로 매칭 서버가 혼잡합니다. 잠시 후 다시 시도해주세요.");
            }

            cleanupUser(requesterId);

            UserInfoDTO requesterInfo = userService.getUserByExternalId(requesterId);
            MatchingProfileDTO requesterProfile = createRequesterProfile(requesterInfo, requesterRegion, request);

            // Redisson RMap 사용
            getUserProfileMap().put(requesterId, requesterProfile);

            findBestPartner(requesterProfile).ifPresentOrElse(
                    partner -> handleMatchSuccess(requesterProfile, partner),
                    () -> addToWaitingQueue(requesterProfile)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while trying to acquire lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * WebRTC 시그널링 메시지를 상대방에게 중계
     * @param senderId 메시지를 보낸 사용자
     * @param message    전송할 SignalMessage
     */
    public void forwardSignalingMessage(String senderId, SignalMessageDTO message) {
        sendSignalingMessage(senderId, message);
    }

    /**
     * 사용자가 연결을 끊었을 때 정리
     */
    public void cleanupUser(String userId) {
        cancelWaitingUser(userId); // 대기열에 있을 때 -> 대기열 및 프로필에서 제거
        leaveChatSession(userId);  // 채팅 세션에서 제거 및 상대방에게 알림
    }

    /* --- 매칭 ---- */

    private Optional<MatchingProfileDTO> findBestPartner(MatchingProfileDTO requester) {
        RList<String> waitingQueue = getWaitingQueue();
        RMap<String, MatchingProfileDTO> userProfileMap = getUserProfileMap();

        if (waitingQueue.isEmpty()) {
            return Optional.empty();
        }

        MatchingProfileDTO bestMatch = null;
        int maxScore = -1;

        for (String partnerId : waitingQueue) {
            MatchingProfileDTO partner = userProfileMap.get(partnerId);

            if (partner == null || !isMatchable(requester, partner)) continue;

            int currentScore = calculateScore(requester, partner);

            if (currentScore > MatchingConstants.MINIMUM_MATCH_SCORE && currentScore > maxScore) {
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
        score += (int) (waitTimeB / 10) * MatchingConstants.WAITING_SCORE_WEIGHT_PER_10_SECONDS;

        return score;
    }

    private void handleMatchSuccess(MatchingProfileDTO userA, MatchingProfileDTO userB) {
        log.info("매칭 성공 userA={}, userB={}", userA, userB);
        getWaitingQueue().remove(userB.getExternalId());

        // DB session 저장
        Session session = sessionService.createSession(userA, userB);

        // Redis에 채팅 세션 기록
        RMap<String, MatchingSessionInfoDTO> chatSessionMap = getChatSessionMap();
        chatSessionMap.putAll(Map.of(
                userA.getExternalId(), new MatchingSessionInfoDTO(session.getExternalId(), userB.getExternalId(), session.getStartedAt()),
                userB.getExternalId(), new MatchingSessionInfoDTO(session.getExternalId(), userA.getExternalId(), session.getStartedAt())
        ));

        // 양쪽 사용자에게 매칭 성공 알림 전송
        sendMatchingResult(userA.getExternalId(), new MatchResultDTO("SUCCESS", userB.getExternalId()));
        sendMatchingResult(userA.getExternalId(), new MatchResultDTO("SUCCESS", userA.getExternalId()));
    }

    private void addToWaitingQueue(MatchingProfileDTO userProfile) {
        log.info("대기열에 추가 userProfile={}", userProfile);
        String userId = userProfile.getExternalId();
        userProfile.setQueueEntryTime(System.currentTimeMillis());

        // 프로필 정보 갱신 후 대기열에 추가
        getUserProfileMap().put(userId, userProfile);
        getWaitingQueue().add(userId);

        MatchResultDTO waitingResult = new MatchResultDTO("WAITING", null);
        sendMatchingResult(userId, waitingResult);
    }

    /* --- 매칭 취소 ---- */

    private void cancelWaitingUser(String userId) {
        RList<String> waitingQueue = getWaitingQueue();

        if (waitingQueue.remove(userId)) { // List에서 성공적으로 제거되면 true 반환
            getUserProfileMap().remove(userId);
        }
    }

    private void leaveChatSession(String userId) {
        RMap<String, MatchingSessionInfoDTO> chatSessionMap = getChatSessionMap();
        MatchingSessionInfoDTO sessionInfo = chatSessionMap.get(userId);

        if (sessionInfo != null) {
            String partnerId = sessionInfo.getPartnerInfo();
            MatchResultDTO payload = new MatchResultDTO("LEAVE", userId);

            // 두 사용자의 세션 제거
            chatSessionMap.fastRemove(userId, partnerId);
            sendMatchingResult(partnerId, payload);

            publishSessionEndedEvent(sessionInfo);
        }
    }

    /* --- 헬퍼 ---- */

    private MatchingProfileDTO createRequesterProfile(UserInfoDTO requesterInfo, String requesterRegion, MatchRequestDTO request) {
        return MatchingProfileDTO.builder()
                .id(requesterInfo.getId())
                .externalId(requesterInfo.getExternalId())
                .region(requesterRegion)
                .gender(requesterInfo.getGender())
                .language(request.language())
                .selectedRegion(request.region())
                .selectedGender(request.gender())
                .build();
    }

    private void publishSessionEndedEvent(MatchingSessionInfoDTO sessionInfo) {
        SessionEndedEvent event = SessionEndedEvent.builder()
                .sessionUuid(sessionInfo.getSessionId())
                .startedAt(sessionInfo.getStartedAt())
                .endedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);
    }

    /* --- Redisson ---- */

    private RMap<String, MatchingProfileDTO> getUserProfileMap() {
        return redissonClient.getMap(MatchingConstants.USER_PROFILE_KEY);
    }

    private RList<String> getWaitingQueue() {
        return redissonClient.getList(MatchingConstants.WAITING_QUEUE_KEY);
    }

    private RMap<String, MatchingSessionInfoDTO> getChatSessionMap() {
        return redissonClient.getMap(MatchingConstants.CHAT_SESSION_KEY);
    }

    /* --- MessagingTemplate ---- */

    private void sendMatchingResult(String userId, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_MATCHING, payload);
    }

    private void sendSignalingMessage(String userId, SignalMessageDTO message) {
        messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_SIGNALING, message);
    }

}
