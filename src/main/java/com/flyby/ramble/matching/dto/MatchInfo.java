package com.flyby.ramble.matching.dto;

import com.flyby.ramble.matching.model.RtcRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매칭 성공 정보")
public record MatchInfo(
        @Schema(description = "세션 ID")
        String sessionId,
        @Schema(description = "WebRTC 역할 (OFFER_USER 또는 ANSWER_USER)")
        RtcRole role,
        @Schema(description = "상대방 사용자 ID")
        String otherUserId
) {
}