package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.CreateUserReportCommandDTO;
import com.flyby.ramble.report.model.UserReport;
import com.flyby.ramble.report.repository.UserReportRepository;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.repository.SessionRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserReportService {
    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public void saveUserReport(CreateUserReportCommandDTO requestDTO) {
        User reportedUser = userRepository.findByExternalId(requestDTO.getReportedUserUuid()).orElseThrow();
        User reportingUser = userRepository.findByExternalId(requestDTO.getReportingUserUuid()).orElseThrow();
        Session session = sessionRepository.findByExternalId(requestDTO.getSessionUuid()).orElseThrow();

        UserReport userReport = UserReport.builder()
                .reportedUser(reportedUser)
                .reportingUser(reportingUser)
                .session(session)
                .reason(requestDTO.getReportReason())
                .detail(requestDTO.getReasonDetail())
                .build();

        userReportRepository.save(userReport);
    }

    public long countByReportedUser(User reportedUser) {
        return userReportRepository.countByReportedUser(reportedUser);
    }
}
