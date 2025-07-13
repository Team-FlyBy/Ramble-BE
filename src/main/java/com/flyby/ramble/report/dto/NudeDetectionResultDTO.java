package com.flyby.ramble.report.dto;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NudeDetectionResultDTO {
    private UUID reportUuid;
    private Boolean isNude;
}
