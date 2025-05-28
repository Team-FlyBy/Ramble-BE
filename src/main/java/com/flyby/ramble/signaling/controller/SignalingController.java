package com.flyby.ramble.signaling.controller;

import com.flyby.ramble.signaling.dto.SignalMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

    @MessageMapping("/signal")
    public void handleSignalMessage(@Payload SignalMessage signalMessage) {
        String receiverId;

        if (signalMessage.getType().equals("offer")) {
            receiverId = waitingQueue.poll();

            if (receiverId == null) {
                waitingQueue.add(signalMessage.getSenderId());
                return;
            }

            signalMessage.setReceiverId(receiverId);
        } else {
            receiverId = signalMessage.getReceiverId();
        }

        messagingTemplate.convertAndSend(String.format("/topic/signal/%s", receiverId), signalMessage);
    }

}
