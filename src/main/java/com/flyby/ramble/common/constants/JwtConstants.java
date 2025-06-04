package com.flyby.ramble.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtConstants {

    /*   JWT Header Keys */

    public static final String REFRESH_COOKIE     = "refresh";
    public static final String TOKEN_PREFIX       = "Bearer ";

    /*   JWT Claim Keys */

    public static final String CLAIM_DEVICE_TYPE  = "deviceType";
    public static final String CLAIM_AUTHORITIES  = "authorities";
    public static final String CLAIM_TOKEN_TYPE   = "type";
    public static final String CLAIM_PROVIDER     = "provider";
    public static final String CLAIM_PROVIDER_ID  = "providerId";

    /*   JWT Token Types */

    public static final String TOKEN_TYPE_ACCESS  = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

}
