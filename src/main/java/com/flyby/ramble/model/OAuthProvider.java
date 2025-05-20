package com.flyby.ramble.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum OAuthProvider {
    GOOGLE,
    APPLE;

    public static OAuthProvider from(String provider) {
        for (OAuthProvider oAuthProvider : OAuthProvider.values()) {
            if (oAuthProvider.name().equalsIgnoreCase(provider)) {
                return oAuthProvider;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + provider);
    }

}
