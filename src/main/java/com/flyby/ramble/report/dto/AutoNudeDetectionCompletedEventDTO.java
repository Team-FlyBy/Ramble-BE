package com.flyby.ramble.report.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoNudeDetectionCompletedEventDTO {
    private UUID userUuid;
    private Boolean isNude;
}
