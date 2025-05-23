package com.flyby.ramble.auth.repository;

import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.common.model.DeviceType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // find by id and not revoked
    Optional<RefreshToken> findByIdAndRevokedFalse(String id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.externalId = :externalId AND rt.deviceType = :deviceType")
    int revokeAllByUserIdAndDeviceType(UUID externalId, DeviceType deviceType);

}