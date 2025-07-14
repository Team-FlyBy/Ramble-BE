package com.flyby.ramble.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthIdTokenDTO(
        @NotBlank(message = "Authorization token is required")
        String token
) {
}
