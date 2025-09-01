package com.flyby.ramble.signaling.controller;

import com.flyby.ramble.matching.dto.SignalMessageDTO;
import com.flyby.ramble.matching.model.SignalType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

    @Deprecated(since = "2025-09-01", forRemoval = true)
    @MessageMapping("/signal")
    public void handleSignalMessage(@Payload SignalMessageDTO signalMessageDTO, Principal principal) {
        String receiverId;
        String senderId = Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자 ID가 유효하지 않습니다."));

        if (signalMessageDTO.getType() == SignalType.OFFER) {
            receiverId = waitingQueue.poll();

            if (receiverId == null) {
                waitingQueue.add(senderId);
                return;
            }
        } else {
            receiverId = signalMessageDTO.getReceiverId();
        }

        signalMessageDTO.setSenderId(senderId);
        messagingTemplate.convertAndSendToUser(receiverId, "/queue/signal", signalMessageDTO);
    }

}
