package com.flyby.ramble.signaling.dto;

import com.flyby.ramble.signaling.model.SignalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(title = "SignalMessage", description = "WebRTC 신호 메시지")
@Data
@NoArgsConstructor
public class SignalMessage {

    private String senderId;
    private String receiverId;
    private SignalType type;
    private Object data;

    @Builder
    public SignalMessage(String senderId, String receiverId, SignalType type, Object data) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.data = data;
    }
}
