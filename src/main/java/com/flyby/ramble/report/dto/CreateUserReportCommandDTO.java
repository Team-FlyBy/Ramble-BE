package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.ReportReason;
import com.flyby.ramble.report.model.UserReportStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateUserReportCommandDTO {
    private Long reportedUserId;
    private Long reportingUserId;
    private Long sessionId;
    private ReportReason reportReason;
    private String reasonDetail;
    private UserReportStatus userReportStatus;

    @Builder
    public CreateUserReportCommandDTO(Long reportedUserId,
                                      Long reportingUserId,
                                      Long sessionId,
                                      ReportReason reportReason,
                                      String reasonDetail,
                                      UserReportStatus userReportStatus) {
        this.reportedUserId = reportedUserId;
        this.reportingUserId = reportingUserId;
        this.sessionId = sessionId;
        this.reportReason = reportReason;
        this.reasonDetail = reasonDetail;
        this.userReportStatus = userReportStatus;
    }
}
