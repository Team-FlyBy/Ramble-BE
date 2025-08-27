package com.flyby.ramble.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * @deprecated 추후 삭제 예정. 클라이언트 수정 후 제거
 */
@Deprecated(since = "2025-08-27", forRemoval = true)
@Schema(title = "OAuth 모바일 인증 DTO (Deprecated)", description = "OAuth 인증에 필요한 데이터를 담은 DTO")
public record OAuthIdTokenDTO(
        @NotBlank(message = "Authorization token is required")
        String token
) {
}
