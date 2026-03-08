package com.flyby.ramble.matching.manager;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.dto.SignalMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingRelayer {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 매칭 결과를 사용자에게 전송
     *
     * @param userId 수신자 ID
     * @param result 매칭 결과 페이로드
     */
    public void sendMatchingResult(String userId, MatchResultDTO result) {
        try {
            messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_MATCHING, result);
            log.debug("매칭 결과 전송: userId={}", userId);
        } catch (Exception e) {
            log.error("매칭 결과 전송 실패: userId={}", userId, e);
        }
    }

    /**
     * 시그널링 메시지를 중계
     * 송신자 ID를 메시지에 설정하고 수신자에게 전송
     *
     * @param senderId 송신자 ID
     * @param message 시그널링 메시지
     */
    public void sendSignalingMessage(String senderId, SignalMessageDTO message) {
        message.setSenderId(senderId);
        relaySignalingMessage(message.getReceiverId(), message);
    }

    /**
     * 시그널링 메시지를 상대방에게 전송
     *
     * @param userId 수신자 ID
     * @param message 시그널링 메시지
     */
    private void relaySignalingMessage(String userId, SignalMessageDTO message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, MatchingConstants.SUBSCRIPTION_SIGNALING, message);
            log.debug("시그널링 메시지 전송: userId={}, type={}", userId, message.getType());
        } catch (Exception e) {
            log.error("시그널링 메시지 전송 실패: userId={}, type={}", userId, message.getType(), e);
        }
    }
}