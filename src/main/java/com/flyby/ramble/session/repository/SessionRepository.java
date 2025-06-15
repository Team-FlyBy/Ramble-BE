package com.flyby.ramble.session.repository;

import com.flyby.ramble.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByExternalId(UUID externalId);
}
