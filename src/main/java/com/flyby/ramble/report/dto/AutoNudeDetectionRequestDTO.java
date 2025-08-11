package com.flyby.ramble.report.dto;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class AutoNudeDetectionRequestDTO {
    private UUID userUuid;
}
