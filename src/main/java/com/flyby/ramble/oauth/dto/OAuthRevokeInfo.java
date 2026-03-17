package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.common.model.OAuthProvider;

public record OAuthRevokeInfo(
        OAuthProvider provider,
        String oauthRefreshToken
) {
}
