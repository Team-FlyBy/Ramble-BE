package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.common.model.OAuthProvider;

public record OAuthRegisterDTO(
        String email,
        String username,
        OAuthProvider provider,
        String providerId
) {
}
