package com.flyby.ramble.report.service;

import com.flyby.ramble.common.service.StorageService;
import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.dto.CreateUserReportCommandDTO;
import com.flyby.ramble.report.dto.DetectNudeCommandDTO;
import com.flyby.ramble.report.dto.ReportUserRequestDTO;
import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.report.model.ReportReason;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.flyby.ramble.report.constants.Constants.MAX_REPORT_COUNT;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final UserRepository userRepository;
    private final UserReportService userReportService;
    private final NudeDetectionService nudeDetectionService;
    private final UserBanService userBanService;

    public void reportByUser(ReportUserRequestDTO requestDTO, MultipartFile peerVideoSnapshot) {
        CreateUserReportCommandDTO commandDTO = CreateUserReportCommandDTO.builder()
                .sessionUuid(requestDTO.getSessionUuid())
                .reportedUserUuid(requestDTO.getReportedUserUuid())
                .reportingUserUuid(requestDTO.getReportingUserUuid())
                .reportReason(requestDTO.getReportReason())
                .reasonDetail(requestDTO.getReasonDetail())
                .build();

        UUID reportUuid = userReportService.saveUserReport(commandDTO);

        User reportedUser = userRepository.findByExternalId(requestDTO.getReportedUserUuid()).orElseThrow();
        long reportCount = userReportService.countByReportedUser(reportedUser);

        // 신고 수 초과 시 바로 정지
        if (reportCount >= MAX_REPORT_COUNT) {
            userBanService.banUser(
                    BanUserCommandDTO.builder()
                            .userUuid(reportedUser.getExternalId())
                            .bannedAt(LocalDateTime.now())
                            .banExpiresAt(LocalDateTime.now().plusDays(3L))
                            .banReason(BanReason.REPORT_ACCUMULATION)
                            .build()
            );
        }

        if (requestDTO.getReportReason() == ReportReason.SEXUAL_CONTENT) {
            nudeDetectionService.requestDetection(
                    DetectNudeCommandDTO.builder()
                            .reportUuid(reportUuid)
                            .peerVideoSnapshot(peerVideoSnapshot)
                            .build()
            );
        }
    }
}
