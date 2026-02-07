package com.flyby.ramble.matching.service;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final QueueManager queueManager;
    private final SessionManager sessionManager;
    private final SignalingRelayer signalingRelayer;

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

        // 기존 데이터 정리
        disconnectUser(userId);
        boolean result = queueManager.enqueue(requesterProfile);

        return result ?
                MatchResultDTO.waiting() :
                MatchResultDTO.failed("대기열 등록에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 사용자가 연결을 끊었을 때 정리
     */
    public void disconnectUser(String userId) {
        // 대기열에 있을 때 -> 대기열 및 프로필에서 제거
        boolean result = queueManager.dequeue(userId);

        // 대기열에 없을 때 -> 채팅 세션에서 제거 및 상대방에게 알림
        if (!result) {
            terminateSession(userId);
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

    // 인스턴스 확장 계획 이전까지는 현재 방식 유지 (현재는 단일 인스턴스)
    @Scheduled(fixedDelay = 2000)
    public void processMatchingQueue() {
        try {
            // 데이터 조회
            Map<String, List<MatchingProfile>> groups = queueManager.pollWithProfiles();

            if (groups.isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();

            // 매칭 처리
            MatchRoundResult round1 = pairCandidates(groups, now);                               // 1단계 매칭 (성별:언어:지역 -> 최대 390개 그룹)
            MatchRoundResult round2 = pairCandidates(regroupRemaining(round1.remaining()), now); // 2단계 매칭 (성별:언어): 매칭 되지 않은 인원(최대 390명, 각 그룹당 1명)을 재그룹해서 매칭
            MatchRoundResult round3 = pairCandidates(regroupRemaining(round2.remaining()), now); // 3단계 매칭 (무작위): 남은 인원 전체(최대 26명)에서 매칭

            List<SessionData> allMatched = new ArrayList<>();
            allMatched.addAll(round1.matched());
            allMatched.addAll(round2.matched());
            allMatched.addAll(round3.matched());

            finalizeMatches(allMatched);
            requeueUnmatched(round3.remaining());
        } catch (Exception e) {
            log.error("매칭 워커 오류 발생", e);
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

        boolean mergeMode = remaining.size() <= 6;

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

        if (profiles.isEmpty()) {
            return;
        }

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
