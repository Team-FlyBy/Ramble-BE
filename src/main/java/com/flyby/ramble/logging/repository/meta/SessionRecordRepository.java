package com.flyby.ramble.logging.repository.meta;

import com.flyby.ramble.logging.model.meta.SessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRecordRepository extends JpaRepository<SessionRecord, Long> {
    Optional<SessionRecord> findByUuid(UUID uuid);
}
