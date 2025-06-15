package com.flyby.ramble.report.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정지 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_bans",
        indexes = {
                @Index(name = "idx_banned_user_released_at", columnList = "banned_user, released_at")
        }
)
public class UserBan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ban_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user", nullable = false)
    private User bannedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BanReason reason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public boolean isActive() {
        return releasedAt == null || releasedAt.isAfter(LocalDateTime.now());
    }
}
