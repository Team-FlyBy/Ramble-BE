package com.flyby.ramble.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(title = "OAuth 인증 DTO", description = "OAuth (Auth Code Flow + PKCE) 인증에 필요한 데이터를 담은 DTO")
public record OAuthPkceDTO(
        @Schema(description = "Authorization code")
        @NotBlank(message = "code는 필수입니다")
        String code,
        @Schema(description = "Code verifier")
        @NotBlank(message = "Code verifier는 필수입니다")
        String codeVerifier,
        @Schema(description = "Redirect URI")
        @NotBlank(message = "Redirect URI는 필수입니다")
        String redirectUri
) {
}
