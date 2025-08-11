package com.flyby.ramble.report.dto;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutoNudeDetectionRequestedEventDTO {
    private UUID userUuid;
    private String fileUrl;
    private String keyUrl;
}
