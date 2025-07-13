package com.flyby.ramble.report.dto;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NudeDetectionRequestDTO {
    private UUID reportId;
    private String fileUrl;
    private String keyUrl;
}
