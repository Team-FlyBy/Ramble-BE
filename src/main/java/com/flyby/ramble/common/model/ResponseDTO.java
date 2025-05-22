package com.flyby.ramble.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "response DTO", description = "응답 DTO")
public record ResponseDTO<T>(
        @Schema(description = "code")     int code,
        @Schema(description = "message")  String message,
        @Schema(description = "body")     T body
) {
}
