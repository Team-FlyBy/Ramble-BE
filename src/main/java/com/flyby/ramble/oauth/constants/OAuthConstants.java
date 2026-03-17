package com.flyby.ramble.oauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthConstants {
    // Google OAuth
    public static final String GOOGLE_JWK_SET_URI    = "https://www.googleapis.com/oauth2/v3/certs";
    public static final String GOOGLE_PEOPLE_API_URL = "https://people.googleapis.com/v1/people/me";
    public static final String GOOGLE_SCOPE_BIRTHDAY = "https://www.googleapis.com/auth/user.birthday.read";
    public static final String GOOGLE_SCOPE_GENDER   = "https://www.googleapis.com/auth/user.gender.read";
    public static final String GOOGLE_REVOKE_URL     = "https://oauth2.googleapis.com/revoke";

    // Apple OAuth
    public static final String APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    public static final String APPLE_TOKEN_URL   = "https://appleid.apple.com/auth/token";
    public static final String APPLE_ISSUER      = "https://appleid.apple.com";
    public static final String APPLE_REVOKE_URL  = "https://appleid.apple.com/auth/revoke";

}
