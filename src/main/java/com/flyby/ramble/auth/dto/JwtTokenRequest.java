package com.flyby.ramble.auth.dto;

import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.model.Role;

import java.util.UUID;

public record JwtTokenRequest(
        UUID jti,
        UUID userExternalId,
        Role role,
        DeviceType deviceType,
        OAuthProvider provider,
        String providerId
) {

    public static JwtTokenRequest of(UserInfoDTO user, DeviceType deviceType) {
        return new JwtTokenRequest(
                UUID.randomUUID(),
                UUID.fromString(user.getExternalId()),
                user.getRole(),
                deviceType,
                user.getProvider(),
                user.getProviderId()
        );
    }
}
