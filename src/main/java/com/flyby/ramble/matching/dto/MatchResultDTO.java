package com.flyby.ramble.matching.dto;

import com.flyby.ramble.matching.model.MatchStatus;
import com.flyby.ramble.matching.model.RtcRole;
import io.swagger.v3.oas.annotations.media.Schema;

// TODO: status enum으로 변경 고려
// TODO: partnerInfo를 UserInfoDTO로 변경 고려

@Schema(description = "Data Transfer Object for Match Result")
public record MatchResultDTO(
        @Schema(description = "결과") MatchStatus status,
        @Schema(description = "역할") RtcRole role,
        @Schema(description = "상대") String partnerInfo
) {
}