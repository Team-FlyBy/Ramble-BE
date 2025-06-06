package com.flyby.ramble.logging.model.meta;


import com.flyby.ramble.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * 세션 메타데이터 엔티티입니다.
 * <p>
 * 세션의 시작 시간, 종료 시간, UUID 등 주요 정보를 저장하여 세션을 추적합니다.
 * <p>
 * - UUID는 각 세션의 고유 식별자로 사용됩니다.
 * - durationSeconds는 시작과 종료 시간 간의 지속 시간을 초 단위로 저장합니다.
 *
 * @author Dongwoon
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "session_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_record_uuid", columnNames = "record_uuid")
        },
        indexes = {
                @Index(name = "idx_session_record_uuid", columnList = "record_uuid")
        }
)
public class SessionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id", nullable = false)
    private Long id;

    @Column(name = "record_uuid",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false)
    private UUID uuid;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Builder
    public SessionRecord(UUID uuid, LocalDateTime startedAt, LocalDateTime endedAt) {
        validate(uuid, startedAt, endedAt);

        this.uuid = uuid;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    private void validate(UUID uuid, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID must not be null");
        }
        if (startedAt == null || endedAt == null) {
            throw new IllegalArgumentException("Both startedAt and endedAt must not be null");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must be after startedAt");
        }
    }

    @PrePersist
    @PreUpdate
    private void updateDurationSeconds() {
        if (startedAt != null && endedAt != null) {
            this.durationSeconds = Duration.between(startedAt, endedAt).getSeconds();
        }
    }
}

