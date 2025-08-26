package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.common.model.OAuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OAuthRegisterDTO(
        @NotNull(message = "Provider is required")
        OAuthProvider provider,
        @NotBlank(message = "Provider ID is required")
        String providerId,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Username is required")
        String username,
        String gender,
        LocalDate birthDate
) {
}
