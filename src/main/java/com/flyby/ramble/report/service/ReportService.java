package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.dto.CreateUserReportCommandDTO;
import com.flyby.ramble.report.dto.DetectNudeCommandDTO;
import com.flyby.ramble.report.dto.ReportUserRequestDTO;
import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.report.model.ReportReason;
import com.flyby.ramble.report.model.UserReportStatus;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.flyby.ramble.report.constants.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final UserRepository userRepository;
    private final UserReportService userReportService;
    private final SessionService sessionService;
    private final NudeDetectionService nudeDetectionService;

    public void reportByUser(ReportUserRequestDTO requestDTO, MultipartFile peerVideoSnapshot) {
        Long sessionId = sessionService.getSessionIdBySessionUuid(requestDTO.getSessionUuid());
        User reportedUser = userRepository.findByExternalId(requestDTO.getReportedUserUuid()).orElseThrow();
        User reportingUser = userRepository.findByExternalId(requestDTO.getReportingUserUuid()).orElseThrow();

        UserReportStatus userReportStatus = UserReportStatus.PENDING;
        boolean isUserCurrentlyBanned = userReportService.isUserCurrentlyBanned(reportedUser.getId());

        if (isUserCurrentlyBanned) {
            // 유저가 이미 밴 당한 상태인 경우 신고 처리됨으로 설정
            userReportStatus = UserReportStatus.RESOLVED;
        }

        CreateUserReportCommandDTO commandDTO = CreateUserReportCommandDTO.builder()
                .sessionId(sessionId)
                .reportedUserId(reportedUser.getId())
                .reportingUserId(reportingUser.getId())
                .reportReason(requestDTO.getReportReason())
                .reasonDetail(requestDTO.getReasonDetail())
                .userReportStatus(userReportStatus)
                .build();

        Long reportId = userReportService.saveUserReport(commandDTO);   // 유저 신고 저장

        if (isUserCurrentlyBanned) {
            // 유저가 이미 밴당한 상태인 경우 그냥 종료
            log.info("This user is already banned - Banned user ID: {}", reportedUser.getId());
            return;
        }

        long reportCount = userReportService.countByUserIdAndStatusIsPending(reportedUser.getId());

        if (reportCount > MAX_REPORT_COUNT) {   // 신고 수 초과 시 바로 정지
            userReportService.banUser(
                    BanUserCommandDTO.builder()
                            .userId(reportedUser.getId())
                            .bannedAt(LocalDateTime.now())
                            .banReason(BanReason.REPORT_ACCUMULATION)
                            .build()
            );

            log.info("User has been banned due to exceeding the allowed number of reports - Banned user ID: {}", reportedUser.getId());
            return;
        }

        if (requestDTO.getReportReason() == ReportReason.SEXUAL_CONTENT) {      // 음란물, 노출 신고
            UUID reportUuid = userReportService.getReportUuidByReportId(reportId);

            nudeDetectionService.requestDetection(
                    DetectNudeCommandDTO.builder()
                            .reportUuid(reportUuid)
                            .peerVideoSnapshot(peerVideoSnapshot)
                            .build()
            );
        }
    }

}
