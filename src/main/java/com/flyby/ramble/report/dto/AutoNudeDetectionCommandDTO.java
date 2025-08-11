package com.flyby.ramble.report.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class AutoNudeDetectionCommandDTO {
    private UUID userUuid;
    private MultipartFile peerVideoSnapshot;
}
