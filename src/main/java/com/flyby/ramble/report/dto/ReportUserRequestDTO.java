package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.ReportReason;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReportUserRequestDTO {
    private UUID reportedUserUuid;
    private UUID reportingUserUuid;
    private UUID sessionUuid;
    private ReportReason reportReason;
    private String reasonDetail;

    @Override
    public String toString() {
        return "ReportUserRequestDTO{" +
                "reportedUserUuid=" + reportedUserUuid +
                ", reportingUserUuid=" + reportingUserUuid +
                ", sessionUuid=" + sessionUuid +
                ", reportReason=" + reportReason +
                ", reasonDetail='" + (reasonDetail != null ? reasonDetail.replaceAll("\n", " ") : "") + '\'' +
                '}';
    }
}
