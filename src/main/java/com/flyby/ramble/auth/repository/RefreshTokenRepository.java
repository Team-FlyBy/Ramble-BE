package com.flyby.ramble.auth.repository;

import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.common.model.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByIdAndRevokedFalse(UUID id);

    void deleteAllByExpiresAtBeforeOrRevokedTrue(LocalDateTime localDateTime);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.externalId = :externalId")
    int revokeAllByUserExternalId(UUID externalId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.externalId = :externalId AND rt.deviceType = :deviceType")
    int revokeAllByUserIdAndDeviceType(UUID externalId, DeviceType deviceType);

}