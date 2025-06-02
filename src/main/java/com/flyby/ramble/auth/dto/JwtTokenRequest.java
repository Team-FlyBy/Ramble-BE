package com.flyby.ramble.auth.dto;


import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Role;
import com.flyby.ramble.user.model.User;

import java.util.UUID;

public record JwtTokenRequest(
        UUID jti,
        UUID userExternalId,
        Role role,
        DeviceType deviceType,
        OAuthProvider provider,
        String providerId
) {

    public static JwtTokenRequest of(User user, DeviceType deviceType) {
        return new JwtTokenRequest(
                UUID.randomUUID(),
                user.getExternalId(),
                user.getRole(),
                deviceType,
                user.getProvider(),
                user.getProviderId()
        );
    }
}
