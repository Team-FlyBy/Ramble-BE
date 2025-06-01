package com.flyby.ramble.auth.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "refresh_tokens",
        indexes = {
            @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
            @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        })
public class RefreshToken extends BaseEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private DeviceType deviceType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Builder
    public RefreshToken(UUID id, User user, DeviceType deviceType, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.deviceType = deviceType;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public RefreshToken revoke() {
        this.revoked = true;
        return this;
    }

}
