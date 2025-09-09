package com.flyby.ramble.matching.service;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.dto.SignalMessageDTO;
import com.flyby.ramble.matching.model.MatchingProfile;
import com.flyby.ramble.matching.model.MatchingSessionInfo;
import com.flyby.ramble.session.event.SessionEndedEvent;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.condition.Conditions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MatchingService {
    private final RedissonClient redissonClient;
    private final RLiveObjectService liveObjectService;
    private final RScoredSortedSet<String> matchingPoolIndex;

    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final UserService userService;
    private final SessionService sessionService;

    // TODO: 추후 책임 분리. 점수 계산, 매칭 로직, 세션 관리, 시그널 중계 등 분할
    // TODO: Redisson Live Object (대기열) 만료 -> 사용자에게 알림 (매칭 실패)
    // TODO: 전역 락 방식 개선

    public MatchingService(RedissonClient redissonClient,
                           SimpMessagingTemplate messagingTemplate,
                           ApplicationEventPublisher eventPublisher,
                           UserService userService,
                           SessionService sessionService) {
        this.redissonClient = redissonClient;
        this.liveObjectService = redissonClient.getLiveObjectService();
        this.matchingPoolIndex = redissonClient.getScoredSortedSet(MatchingConstants.MATCHMAKING_POOL_KEY);

        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;

        this.userService = userService;
        this.sessionService = sessionService;
    }

    /**
     * 신규 사용자의 매칭을 요청하거나 대기열에 추가
     */
    public void findMatchOrAddToQueue(String requesterId, String requesterRegion, MatchRequestDTO request) {
        RLock lock = redissonClient.getLock(MatchingConstants.MATCHMAKING_LOCK_KEY);

        UserInfoDTO requesterInfo = userService.getUserByExternalId(requesterId);
        MatchingProfile requesterProfile = createRequesterProfile(requesterInfo, requesterRegion, request);

        try {
            boolean isLocked = lock.tryLock(MatchingConstants.LOCK_WAIT_SECONDS, MatchingConstants.LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("매칭 락 획득 실패: {}", requesterId);
                throw new IllegalStateException("일시적으로 매칭 서버가 혼잡합니다. 잠시 후 다시 시도해주세요.");
            }

            // 기존 대기열 또는 채팅 세션 정리
            cleanupUser(requesterId);

            // 최적의 파트너 탐색
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
        message.setSenderId(senderId);
        sendSignalingMessage(message.getReceiverId(), message);
    }

    /**
     * 사용자가 연결을 끊었을 때 정리
     */
    public void cleanupUser(String userId) {
        cancelWaitingUser(userId); // 대기열에 있을 때 -> 대기열 및 프로필에서 제거
        leaveChatSession(userId);  // 채팅 세션에서 제거 및 상대방에게 알림
    }

    /* --- 매칭 ---- */

    private Optional<MatchingProfile> findBestPartner(MatchingProfile requester) {
        Collection<String> waitingQueue = matchingPoolIndex.valueRange(0, MatchingConstants.MAX_WAITING_QUEUE_SIZE - 1);

        MatchingProfile bestMatch = null;
        int maxScore = -1;

        for (String partnerId : waitingQueue) {
            MatchingProfile partner = liveObjectService.get(MatchingProfile.class, partnerId);

            if (partner == null || !isMatchable(requester, partner)) continue;

            int currentScore = calculateScore(requester, partner);

            if (currentScore >= MatchingConstants.MINIMUM_MATCH_SCORE && currentScore > maxScore) {
                maxScore = currentScore;
                bestMatch = partner;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    private boolean isMatchable(MatchingProfile userA, MatchingProfile userB) {
        return !Objects.equals(userA.getUserExternalId(), userB.getUserExternalId())
                && !isBlocked(userA.getUserExternalId(), userB.getUserExternalId());
    }

    private boolean isBlocked(String aExternalId, String bExternalId) {
        // 차단/신고 로직 미구현 -> 현재는 항상 false
        return false;
    }

    private int calculateScore(MatchingProfile userA, MatchingProfile userB) {
        int score = 0;

        // 지역 선호 매칭 (상대방 실제 지역이 내가 선택한 지역)
        if (Objects.equals(userA.getSelectedRegion(), userB.getRegion())) score += MatchingConstants.REGION_MATCH_SCORE;
        if (Objects.equals(userB.getSelectedRegion(), userA.getRegion())) score += MatchingConstants.REGION_MATCH_SCORE;
        // 동일 실제 지역 (부분 매칭)
        if (Objects.equals(userA.getRegion(), userB.getRegion())) score += MatchingConstants.REGION_PARTIAL_MATCH_SCORE;

        // 성별 다를 시 점수 추가 (선택한 성별과 상대방 실제 성별이 다를 시)
        if (!Objects.equals(userA.getSelectedGender(), userB.getGender())) score += MatchingConstants.GENDER_DIFFERENCE_SCORE;
        if (!Objects.equals(userB.getSelectedGender(), userA.getGender())) score += MatchingConstants.GENDER_DIFFERENCE_SCORE;
        // 실제 성별이 다를 시 점수 추가
        if (!Objects.equals(userA.getGender(), userB.getGender())) score += MatchingConstants.GENDER_DIFFERENCE_SCORE;

        // 언어 일치
        if (Objects.equals(userA.getLanguage(), userB.getLanguage())) score += MatchingConstants.LANGUAGE_MATCH_SCORE;

        // 대기 시간에 비례하여 점수 추가 (10초당 1점)
        long currentTime = System.currentTimeMillis();
        long waitTimeB = (currentTime - userB.getQueueEntryTime()) / 1000;
        score += (int) (waitTimeB / 10) * MatchingConstants.WAITING_SCORE_PER_10_SECONDS;
        return score;
    }

    private void handleMatchSuccess(MatchingProfile userA, MatchingProfile userB) {
        // 안전 제거 (존재 시)
        matchingPoolIndex.remove(userB.getUserExternalId());
        MatchingProfile existing = liveObjectService.get(MatchingProfile.class, userB.getUserExternalId());
        if (existing != null) {
            liveObjectService.delete(existing);
        }

        // DB session 저장
        Session session = sessionService.createSession(userA, userB);

        // Redis에 채팅 세션 기록
        MatchingSessionInfo liveSession = MatchingSessionInfo.builder()
                .sessionId(session.getExternalId().toString())
                .participantAId(userA.getUserExternalId())
                .participantBId(userB.getUserExternalId())
                .build();
        liveSession = liveObjectService.persist(liveSession);
        liveObjectService.asLiveObject(liveSession).expire(Duration.ofMinutes(5)); // 5분 후 자동 만료

        // 양쪽 사용자에게 매칭 성공 알림 전송
        sendMatchingResult(userA.getUserExternalId(), new MatchResultDTO("SUCCESS", userB.getUserExternalId()));
        sendMatchingResult(userB.getUserExternalId(), new MatchResultDTO("SUCCESS", userA.getUserExternalId()));
    }

    private void addToWaitingQueue(MatchingProfile userProfile) {
        long now = System.currentTimeMillis();

        // Redis 저장 (대기열)
        userProfile.setQueueEntryTime(now);
        liveObjectService.persist(userProfile);
        matchingPoolIndex.add(now, userProfile.getUserExternalId());

        // 대기 상태 알림 전송
        sendMatchingResult(userProfile.getUserExternalId(), new MatchResultDTO("WAITING", null));
    }

    /* --- 매칭 취소 ---- */

    private void cancelWaitingUser(String userId) {
        MatchingProfile userProfile = liveObjectService.get(MatchingProfile.class, userId);

        if (userProfile != null) {
            liveObjectService.delete(userProfile);
            matchingPoolIndex.remove(userId);
        }
    }

    private void leaveChatSession(String userId) {
        MatchingSessionInfo sessionInfo = findSessionByParticipantId(userId);

        if (sessionInfo != null) {
            String partnerId = sessionInfo.getParticipantAId().equals(userId)
                    ? sessionInfo.getParticipantBId()
                    : sessionInfo.getParticipantAId();
            MatchResultDTO payload = new MatchResultDTO("LEAVE", userId);

            // 두 사용자의 세션 제거
            liveObjectService.delete(sessionInfo);
            // 상대방에게 종료 알림 전송
            sendMatchingResult(partnerId, payload);
            // 세션 종료 이벤트 발행
            publishSessionEndedEvent(sessionInfo);
        }
    }

    /* --- 헬퍼 ---- */

    private MatchingProfile createRequesterProfile(UserInfoDTO requesterInfo, String requesterRegion, MatchRequestDTO request) {
        return MatchingProfile.builder()
                .userId(requesterInfo.getId())
                .userExternalId(requesterInfo.getExternalId())
                .region(requesterRegion)
                .gender(requesterInfo.getGender())
                .language(request.language())
                .selectedRegion(request.region())
                .selectedGender(request.gender())
                .build();
    }

    private void publishSessionEndedEvent(MatchingSessionInfo sessionInfo) {
        SessionEndedEvent event = SessionEndedEvent.builder()
                .sessionUuid(UUID.fromString(sessionInfo.getSessionId()))
                .startedAt(sessionInfo.getStartedAt())
                .endedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * QUERY: 참가자 ID(인덱스)로 조회
     */
    public MatchingSessionInfo findSessionByParticipantId(String participantId) {
        Collection<MatchingSessionInfo> sessions = liveObjectService.find(MatchingSessionInfo.class,
                Conditions.or(
                        Conditions.eq("participantAId", participantId),
                        Conditions.eq("participantBId", participantId)
                ));

        return sessions.stream().findFirst().orElse(null);
    }

    /* --- MessagingTemplate ---- */

    private void sendMatchingResult(String userId, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_MATCHING, payload);
    }

    private void sendSignalingMessage(String userId, SignalMessageDTO message) {
        messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_SIGNALING, message);
    }

}
