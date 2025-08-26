package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReportUserRequestDTO {
    @NotNull
    private UUID reportedUserUuid;

    @NotNull
    private UUID reportingUserUuid;

    @NotNull
    private UUID sessionUuid;

    @NotNull
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
