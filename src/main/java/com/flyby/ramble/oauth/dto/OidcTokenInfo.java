package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.common.model.OAuthProvider;

public record OidcTokenInfo(
        OAuthProvider provider,
        String providerId,
        String email,
        String username
) {
}
