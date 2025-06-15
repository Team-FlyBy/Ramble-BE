package com.flyby.ramble.report.service;

import com.flyby.ramble.common.service.StorageService;
import com.flyby.ramble.report.dto.CreateUserReportCommandDTO;
import com.flyby.ramble.report.dto.DetectNudeCommandDTO;
import com.flyby.ramble.report.dto.ReportUserRequestDTO;
import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.flyby.ramble.report.constants.Constants.MAX_REPORT_COUNT;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final UserRepository userRepository;
    private final UserReportService userReportService;
    private final NudeDetectionService nudeDetectionService;
    private final StorageService storageService;
    private final UserBanService userBanService;

    public void reportByUser(ReportUserRequestDTO requestDTO, MultipartFile peerVideoSnapshot) {
        CreateUserReportCommandDTO commandDTO = CreateUserReportCommandDTO.builder()
                .sessionUuid(requestDTO.getSessionUuid())
                .reportedUserUuid(requestDTO.getReportedUserUuid())
                .reportingUserUuid(requestDTO.getReportingUserUuid())
                .reportReason(requestDTO.getReportReason())
                .reasonDetail(requestDTO.getReasonDetail())
                .build();

        userReportService.saveUserReport(commandDTO);

        User reportedUser = userRepository.findByExternalId(requestDTO.getReportedUserUuid()).orElseThrow();
        long reportCount = userReportService.countByReportedUser(reportedUser);

        // 신고 수 초과 시 바로 정지
        if (reportCount >= MAX_REPORT_COUNT) {
            userBanService.banUser(requestDTO.getReportedUserUuid(), BanReason.REPORT_ACCUMULATION);
        }

        /**
         * TODO: 신고 시 가져온 상대방 화상 스냅샷을 어떻게 AI 서버로 전송할 지 논의 필요. 압축 및 암호화는 기본 필요
         * TODO: S3에 저장할지 아니면 바로 전송해서 분석하고 지울 지
         */
        String peerVideoSnapshotUrl = storageService.uploadFile("", peerVideoSnapshot);

        // TODO: 업로드 후 해당 파일 주소로 누드 감지 기능 사용
        nudeDetectionService.detect(
                DetectNudeCommandDTO.builder()
                        .sessionUuid(requestDTO.getSessionUuid())
                        .reportingUserUuid(requestDTO.getReportingUserUuid())
                        .reportedUserUuid(requestDTO.getReportedUserUuid())
                        .snapshotUrl(peerVideoSnapshotUrl)
                        .build()
        );
    }
}
