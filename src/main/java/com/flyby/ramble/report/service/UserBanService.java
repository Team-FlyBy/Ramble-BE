package com.flyby.ramble.report.service;

import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.report.repository.UserReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBanService {
    private final UserReportRepository userReportRepository;

    @Transactional
    public void banUser(UUID userUuid, BanReason banReason) {
        // TEMP
    }
}
