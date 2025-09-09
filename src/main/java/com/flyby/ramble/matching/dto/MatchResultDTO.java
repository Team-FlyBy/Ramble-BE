package com.flyby.ramble.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// TODO: status enum으로 변경 고려
// TODO: partnerInfo를 UserInfoDTO로 변경 고려

@Schema(description = "Data Transfer Object for Match Result")
public record MatchResultDTO(
        @Schema(description = "결과") String status,
        @Schema(description = "상대") String partnerInfo
) {
}