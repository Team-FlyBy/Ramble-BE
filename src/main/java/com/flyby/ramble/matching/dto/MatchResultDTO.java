package com.flyby.ramble.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyby.ramble.matching.model.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매칭 결과")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResultDTO(
        @Schema(description = "매칭 상태", example = "SUCCESS")
        MatchStatus status,
        @Schema(description = "매칭 데이터 (성공 시에만 존재)")
        MatchInfo data,
        @Schema(description = "상태 메시지", example = "매칭에 성공했습니다.")
        String message
) {
    /**
     * 매칭 성공 결과 생성
     */
    public static MatchResultDTO success(MatchInfo data) {
        return new MatchResultDTO(MatchStatus.SUCCESS, data, "매칭에 성공했습니다.");
    }

    /**
     * 대기 중 결과 생성
     */
    public static MatchResultDTO waiting() {
        return new MatchResultDTO(MatchStatus.WAITING, null, "적합한 상대를 찾고 있습니다.");
    }

    /**
     * 매칭 실패 결과 생성
     */
    public static MatchResultDTO failed(String reason) {
        return new MatchResultDTO(
                MatchStatus.FAILED,
                null,
                reason != null ? reason : "매칭에 실패했습니다."
        );
    }

    /**
     * 상대방 퇴장 결과 생성
     */
    public static MatchResultDTO leave() {
        return new MatchResultDTO(MatchStatus.LEAVE, null, "상대방이 세션을 종료했습니다.");
    }

}