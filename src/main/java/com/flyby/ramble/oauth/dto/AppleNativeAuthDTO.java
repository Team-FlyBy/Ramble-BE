package com.flyby.ramble.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Apple Native OAuth 인증 DTO", description = "Apple Native OAuth 인증에 필요한 데이터를 담은 DTO")
public record AppleNativeAuthDTO(
        @Schema(description = "Authorization code")
        String authorizationCode,
        @Schema(description = "id token")
        String identityToken,
        @Schema(description = "email")
        String email,
        @Schema(description = "name")
        String name
) {
}
