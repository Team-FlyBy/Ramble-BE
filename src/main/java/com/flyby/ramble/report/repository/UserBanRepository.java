package com.flyby.ramble.report.repository;

import com.flyby.ramble.report.model.UserBan;
import com.flyby.ramble.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, Long>, UserBanRepositoryCustom {
    Long countByBannedUser(User bannedUser);
}
