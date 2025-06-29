package com.flyby.ramble.oauth.dto;

public record OAuthRequestDTO(
        String code,
        String codeVerifier,
        String redirectUri
) {
}
