package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.BanReason;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BanUserCommandDTO {
    private UUID userUuid;
    private BanReason banReason;
    private LocalDateTime bannedAt;
    private LocalDateTime banExpiresAt;
}
