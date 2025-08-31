package com.flyby.ramble.matching.dto;

import com.flyby.ramble.user.model.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data Transfer Object for Match Request")
public record MatchRequestDTO(
        @Schema(description = "사용자가 선택한 지역") String region,
        @Schema(description = "사용자가 선택한 성별") Gender gender,
        @Schema(description = "사용자가 설정한 앱, 웹 언어") String language
) {
}
