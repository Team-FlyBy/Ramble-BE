package com.flyby.ramble.matching.service;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.*;
import com.flyby.ramble.matching.manager.QueueManager;
import com.flyby.ramble.matching.manager.SessionManager;
import com.flyby.ramble.matching.manager.SignalingRelayer;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.model.RtcRole;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final QueueManager queueManager;
    private final SessionManager sessionManager;
    private final SignalingRelayer signalingRelayer;
    private final RedissonClient redissonClient;

    private final UserService userService;

    // TODO: MatchResultDTO.failed 에러 메시지 추후 관리

    public MatchResultDTO requestMatch(
            String userId,
            Region region,
            MatchRequestDTO request
    ) {
        if (request == null) {
            return MatchResultDTO.failed("매칭 요청 데이터가 누락되었습니다.");
        }

        UserInfoDTO requesterInfo = userService.getUserByExternalId(userId);
        MatchingProfile requesterProfile = buildMatchingProfile(requesterInfo, region, request);

        RLock lock = redissonClient.getLock(MatchingConstants.MATCHING_LOCK);

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("매칭 요청 락 획득 실패: userId={}", userId);
                return MatchResultDTO.failed("현재 서버가 혼잡합니다. 잠시 후 다시 시도해주세요.");
            }

            disconnectUser(userId, System.currentTimeMillis());
            boolean result = queueManager.enqueue(requesterProfile);

            return result ?
                    MatchResultDTO.waiting() :
                    MatchResultDTO.failed("대기열 등록에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MatchResultDTO.failed("매칭 요청 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 사용자가 연결을 끊었을 때 정리
     *
     * @param userId 사용자 ID
     * @param disconnectTimestamp 연결 해제 이벤트 발생 시점 (밀리초).
     *                           enqueue 시점보다 이전이면 stale 이벤트로 판단하여 dequeue를 건너뜀.
     */
    public void disconnectUser(String userId, long disconnectTimestamp) {
        RLock lock = redissonClient.getLock(MatchingConstants.MATCHING_LOCK);

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("disconnectUser 락 획득 실패: userId={}", userId);
                return;
            }

            MatchingProfile profile = queueManager.getProfile(userId);

            if (profile != null && profile.getQueueEntryTime() > disconnectTimestamp) {
                log.info("Stale disconnect 무시: userId={}, enqueue={}ms > disconnect={}ms",
                        userId, profile.getQueueEntryTime(), disconnectTimestamp);
                return;
            }

            boolean dequeued = queueManager.dequeue(userId);

            if (!dequeued) {
                terminateSession(userId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("disconnectUser 처리 중 인터럽트 발생: userId={}", userId);
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
    public void relaySignal(String senderId, SignalMessageDTO message) {
        SessionData session = sessionManager.getSessionByUserId(senderId);
        String partnerId = sessionManager.findPartnerId(session, senderId);

        if (partnerId == null) {
            log.warn("시그널 중계 실패: 활성 세션 없음 [sender={}, type={}]", senderId, message.getType());
            return;
        }

        if (!partnerId.equals(message.getReceiverId())) {
            log.warn("시그널 중계 거부: 수신자 불일치 [sender={}, receiver={}, expectedPartner={}]",
                    senderId, message.getReceiverId(), partnerId);
            return;
        }

        signalingRelayer.sendSignalingMessage(senderId, message);
    }

    /* --- 매칭 ---- */

    @Scheduled(fixedDelay = 2000)
    public void processMatchingQueue() {
        RLock lock = redissonClient.getLock(MatchingConstants.MATCHING_LOCK);

        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                return;
            }

            // 데이터 조회
            Map<String, List<MatchingProfile>> groups = queueManager.pollWithProfiles();

            if (groups.isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();

            // 매칭 처리
            MatchRoundResult round1 = pairCandidates(groups, now);                               // 1단계 매칭 (성별:언어:지역 -> 최대 390개 그룹)
            MatchRoundResult round2 = pairCandidates(regroupRemaining(round1.remaining()), now); // 2단계 매칭 (성별:언어): 매칭 되지 않은 인원(최대 390명, 각 그룹당 1명)을 재그룹해서 매칭
            MatchRoundResult round3 = pairCandidates(regroupRemaining(round2.remaining()), now); // 3단계 매칭 (무작위): 남은 인원 전체(최대 26명)에서 매칭

            List<SessionData> allMatched = Stream.of(round1, round2, round3)
                    .flatMap(r -> r.matched().stream())
                    .toList();

            finalizeMatches(allMatched);
            requeueUnmatched(round3.remaining());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("매칭 워커 오류 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private MatchRoundResult pairCandidates(
            Map<String, List<MatchingProfile>> groups,
            LocalDateTime now
    ) {
        if (groups.isEmpty()) {
            return new MatchRoundResult(Collections.emptyList(), Collections.emptyMap());
        }

        List<SessionData> matchedList = new ArrayList<>();
        Map<String, MatchingProfile> remaining = new HashMap<>();

        // 1번 방법
        groups.forEach((key, value) -> {
            Iterator<MatchingProfile> candidates = value.iterator();

            while (candidates.hasNext()) {
                MatchingProfile profile = candidates.next();

                if (candidates.hasNext()) { // 짝이 있는 경우: 매칭 리스트에 추가
                    matchedList.add(new SessionData(now, List.of(
                            ParticipantData.from(profile),
                            ParticipantData.from(candidates.next())
                    )));
                } else { // 짝이 없는 경우(홀수): remaining 맵에 추가
                    remaining.put(key, profile);
                }
            }
        });

        return new MatchRoundResult(matchedList, remaining);
    }

    /**
     * 하나씩 남은 그룹에 대해서 리그룹 진행
     */
    private Map<String, List<MatchingProfile>> regroupRemaining(Map<String, MatchingProfile> remaining) {
        if (remaining == null || remaining.isEmpty()) {
            return Collections.emptyMap();
        }

        boolean mergeMode = remaining.size() <= 6; // 임시값. 현재는 유지

        // 2. Stream을 이용한 일괄 처리
        return remaining.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> {
                            if (mergeMode) return "MergedGroup";

                            // (성별:언어:지역) 조건에서 마지막 조건 제거
                            String key = entry.getKey();
                            int lastIdx = key.lastIndexOf(':');
                            return (lastIdx == -1) ? key : key.substring(0, lastIdx);
                        },
                        Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(LinkedList::new))
                ));
    }

    private void finalizeMatches(List<SessionData> matchedList) {
        if (matchedList.isEmpty()) {
            return;
        }

        // 세션 생성 (DB + Redis)
        sessionManager.saveSessions(matchedList);

        // 매칭된 프로필(userExternalId) 리스트
        Set<String> list = matchedList.stream()
                .flatMap(session -> session.participants().stream())
                .map(ParticipantData::userExternalId)
                .collect(Collectors.toSet());

        queueManager.deleteProfiles(list);

        // 매칭 결과 전송
        for (SessionData session : matchedList) {
            List<ParticipantData> p = session.participants();
            String sessionId = session.sessionId().toString();

            signalingRelayer.sendMatchingResult(p.get(0).userExternalId(),
                    MatchResultDTO.success(new MatchInfo(sessionId, RtcRole.OFFER_USER, p.get(1).userExternalId())));
            signalingRelayer.sendMatchingResult(p.get(1).userExternalId(),
                    MatchResultDTO.success(new MatchInfo(sessionId, RtcRole.ANSWER_USER, p.get(0).userExternalId())));
        }
    }

    private void requeueUnmatched(Map<String, MatchingProfile> remaining) {
        if (remaining.isEmpty()) {
            return;
        }

        List<MatchingProfile> profiles = remaining.values().stream().toList();

        // 매칭 후 인원이 남았다면 다시 큐에 삽입
        queueManager.requeueAll(profiles);
    }

    private void terminateSession(String userId) {
        SessionData session = sessionManager.getSessionByUserId(userId);
        String partnerId = sessionManager.findPartnerId(session, userId);

        if (partnerId == null) {
            return;
        }

        // 세션 삭제
        sessionManager.closeSession(session);
        // 상대방에게 종료 알림 전송
        signalingRelayer.sendMatchingResult(partnerId, MatchResultDTO.leave());
    }

    /* --- 헬퍼 ---- */

    /**
     * 요청자 프로필 생성
     *
     * @param requesterInfo 요청자 정보
     * @param requesterRegion 요청자 지역
     * @param request 매칭 요청
     * @return 매칭 프로필
     */
    private MatchingProfile buildMatchingProfile(
            UserInfoDTO requesterInfo,
            Region requesterRegion,
            MatchRequestDTO request
    ) {
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

}
