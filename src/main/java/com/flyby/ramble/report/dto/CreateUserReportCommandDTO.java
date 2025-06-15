package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.ReportReason;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateUserReportCommandDTO {
    private UUID reportedUserUuid;
    private UUID reportingUserUuid;
    private UUID sessionUuid;
    private ReportReason reportReason;
    private String reasonDetail;

    @Builder
    public CreateUserReportCommandDTO(UUID reportedUserUuid,
                                      UUID reportingUserUuid,
                                      UUID sessionUuid,
                                      ReportReason reportReason,
                                      String reasonDetail) {
        this.reportedUserUuid = reportedUserUuid;
        this.reportingUserUuid = reportingUserUuid;
        this.sessionUuid = sessionUuid;
        this.reportReason = reportReason;
        this.reasonDetail = reasonDetail;
    }
}
