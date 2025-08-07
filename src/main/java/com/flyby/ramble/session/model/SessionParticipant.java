package com.flyby.ramble.session.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 참여자 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "session_participants",
        indexes = {
                @Index(name = "idx_session_user", columnList = "session_id, user_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_session_user", columnNames = {"session_id", "user_id"})
        }
)
public class SessionParticipant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public SessionParticipant(Session session, User user) {
        this.session = session;
        this.user = user;
    }
}
