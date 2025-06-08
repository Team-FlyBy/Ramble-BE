package com.flyby.ramble.session.model;

import com.flyby.ramble.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
}
