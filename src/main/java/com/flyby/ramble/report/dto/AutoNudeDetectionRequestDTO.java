package com.flyby.ramble.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class AutoNudeDetectionRequestDTO {
    @NotNull
    private UUID userUuid;
}
