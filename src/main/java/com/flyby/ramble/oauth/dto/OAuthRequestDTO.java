package com.flyby.ramble.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthRequestDTO(
        @NotBlank(message = "Authorization code is required")
        String code,
        @NotBlank(message = "Code verifier is required")
        String codeVerifier,
        @NotBlank(message = "Redirect URI is required")
        String redirectUri
) {
}
