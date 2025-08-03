package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.dto.CreateUserReportCommandDTO;
import com.flyby.ramble.report.model.UserReport;
import com.flyby.ramble.report.model.UserReportStatus;
import com.flyby.ramble.report.repository.UserReportRepository;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReportService {
    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final SessionRepository sessionRepository;
    private final UserBanService userBanService;

    @Transactional
    public Long saveUserReport(CreateUserReportCommandDTO commandDTO) {
        User reportedUser = userRepository.findById(commandDTO.getReportedUserId()).orElseThrow();
        User reportingUser = userRepository.findById(commandDTO.getReportingUserId()).orElseThrow();
        Session session = sessionRepository.findById(commandDTO.getSessionId()).orElseThrow();

        UserReport userReport = UserReport.builder()
                .reportedUser(reportedUser)
                .reportingUser(reportingUser)
                .session(session)
                .reason(commandDTO.getReportReason())
                .detail(commandDTO.getReasonDetail())
                .status(commandDTO.getUserReportStatus())
                .build();

        userReportRepository.save(userReport);

        return userReport.getId();
    }

    @Transactional
    public void banUser(BanUserCommandDTO commandDTO) {
        User bannedUser = userRepository.findById(commandDTO.getUserId()).orElseThrow();
        userBanService.banUser(commandDTO);

        //// 쿼리 최적화, QueryDSL로 대체 예정
        List<UserReport> userReportList = userReportRepository.findAllByReportedUserAndStatusIs(bannedUser, UserReportStatus.PENDING);

        for (UserReport userReport : userReportList) {
            userReport.setStatus(UserReportStatus.RESOLVED);
        }

        userReportRepository.saveAll(userReportList);
        ////
    }

    public boolean isUserCurrentlyBanned(Long userId) {
        return userBanService.isUserCurrentlyBanned(userId);
    }

    public UUID getReportUuidByReportId(Long reportId) {
        UserReport userReport = userReportRepository.findById(reportId).orElseThrow();
        return userReport.getExternalId();
    }

    public long countByUserIdAndStatusIsPending(Long userId) {
        User reportedUser = userRepository.findById(userId).orElseThrow();
        return userReportRepository.countByReportedUserAndStatusIs(reportedUser, UserReportStatus.PENDING);
    }
}
