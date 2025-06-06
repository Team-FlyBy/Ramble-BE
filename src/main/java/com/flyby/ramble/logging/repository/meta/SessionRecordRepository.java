package com.flyby.ramble.logging.repository.meta;

import com.flyby.ramble.logging.model.meta.SessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRecordRepository extends JpaRepository<SessionRecord, Long> {
}
