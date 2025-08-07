package com.flyby.ramble.report.repository;

import com.flyby.ramble.report.model.UserReport;
import com.flyby.ramble.report.model.UserReportStatus;
import com.flyby.ramble.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    List<UserReport> findAllByReportedUserAndStatusIs(User reportedUser, UserReportStatus status);
    Optional<UserReport> findById(Long id);
    Optional<UserReport> findByUuid(UUID uuid);
    long countByReportedUserAndStatusIs(User reportedUser, UserReportStatus status);
}
