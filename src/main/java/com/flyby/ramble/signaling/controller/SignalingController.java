package com.flyby.ramble.signaling.controller;

import com.flyby.ramble.signaling.dto.SignalMessage;
import com.flyby.ramble.signaling.model.SignalType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

    @MessageMapping("/signal")
    public void handleSignalMessage(@Payload SignalMessage signalMessage, Principal principal) {
        String senderId = principal.getName();
        String receiverId;

        if (signalMessage.getType() == SignalType.OFFER) {
            receiverId = waitingQueue.poll();

            if (receiverId == null) {
                waitingQueue.add(senderId);
                return;
            }
        } else {
            receiverId = signalMessage.getReceiverId();
        }

        signalMessage.setSenderId(senderId);
        messagingTemplate.convertAndSendToUser(receiverId, "/queue/signal", signalMessage);
    }

}
