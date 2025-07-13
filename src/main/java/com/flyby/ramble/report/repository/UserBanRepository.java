package com.flyby.ramble.report.repository;

import com.flyby.ramble.report.model.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {
}
