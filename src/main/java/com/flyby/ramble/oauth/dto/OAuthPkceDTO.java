package com.flyby.ramble.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "OAuth 인증 DTO", description = "OAuth (Auth Code Flow + PKCE) 인증에 필요한 데이터를 담은 DTO")
public record OAuthPkceDTO(
        @Schema(description = "Authorization code")
        String code,
        @Schema(description = "Code verifier")
        String codeVerifier,
        @Schema(description = "Redirect URI")
        String redirectUri
) {
}
