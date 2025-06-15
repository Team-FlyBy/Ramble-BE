package com.flyby.ramble.report.repository;

import com.flyby.ramble.report.model.UserReport;
import com.flyby.ramble.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    List<UserReport> findAllByReportedUser(User reportedUser);
    long countByReportedUser(User reportedUser);
}
