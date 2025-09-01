package com.flyby.ramble.matching.dto;

import com.flyby.ramble.matching.model.SignalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(title = "SignalMessage", description = "WebRTC 신호 메시지")
@Data
@NoArgsConstructor
public class SignalMessageDTO {

    private String senderId;
    private String receiverId;
    private SignalType type;
    private Object data;

    @Builder
    public SignalMessageDTO(String senderId, String receiverId, SignalType type, Object data) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.data = data;
    }
}
