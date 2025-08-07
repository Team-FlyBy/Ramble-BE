package com.flyby.ramble.report.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectNudeCommandDTO {
    private UUID reportUuid;
    private MultipartFile peerVideoSnapshot;

    @Builder
    public DetectNudeCommandDTO(UUID reportUuid,
                                MultipartFile peerVideoSnapshot) {
        this.reportUuid = reportUuid;
        this.peerVideoSnapshot = peerVideoSnapshot;
    }
}
