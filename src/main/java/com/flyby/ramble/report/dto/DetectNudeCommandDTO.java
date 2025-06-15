package com.flyby.ramble.report.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectNudeCommandDTO {
    private UUID reportedUserUuid;
    private UUID reportingUserUuid;
    private UUID sessionUuid;
    private String snapshotUrl;

    @Builder
    public DetectNudeCommandDTO(UUID reportedUserUuid, UUID reportingUserUuid, UUID sessionUuid, String snapshotUrl) {
        this.reportedUserUuid = reportedUserUuid;
        this.reportingUserUuid = reportingUserUuid;
        this.sessionUuid = sessionUuid;
        this.snapshotUrl = snapshotUrl;
    }
}
