package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.common.model.OAuthProvider;
import jakarta.validation.constraints.NotBlank;

public record OAuthRegisterDTO(
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "Provider is required")
        OAuthProvider provider,
        @NotBlank(message = "Provider ID is required")
        String providerId
) {
}
