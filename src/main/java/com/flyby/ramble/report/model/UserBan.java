package com.flyby.ramble.report.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 정지 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_bans",
        indexes = {
                @Index(name = "idx_banned_user_ban_expires_at", columnList = "banned_user, ban_expires_at")
        }
)
public class UserBan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ban_id")
    private Long id;

    @Column(name = "external_id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false,
            unique = true)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user", nullable = false)
    private User bannedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BanReason reason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt;

    public boolean isActive() {
        return banExpiresAt == null || banExpiresAt.isAfter(LocalDateTime.now());
    }

    @Builder
    public UserBan(User bannedUser, BanReason reason, LocalDateTime bannedAt, LocalDateTime banExpiresAt) {
        this.externalId = UUID.randomUUID();
        this.bannedUser = bannedUser;
        this.reason = reason;
        this.bannedAt = bannedAt;
        this.banExpiresAt = banExpiresAt;
    }
}
