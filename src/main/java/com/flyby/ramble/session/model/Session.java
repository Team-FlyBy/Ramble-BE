package com.flyby.ramble.session.model;

import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sessions")
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "external_id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false,
            unique = true)
    private UUID externalId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SessionParticipant> participants = new ArrayList<>();

    @Builder
    public Session(LocalDateTime startedAt) {
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt은 null일 수 없습니다.");
        }
        this.externalId = UUID.randomUUID();
        this.startedAt = startedAt;
    }

    public void addParticipant(User user, Gender gender, String region, String language) {
        SessionParticipant participant = SessionParticipant.builder()
                .session(this)
                .user(user)
                .gender(gender)
                .region(region)
                .language(language)
                .build();

        this.participants.add(participant);
    }

}