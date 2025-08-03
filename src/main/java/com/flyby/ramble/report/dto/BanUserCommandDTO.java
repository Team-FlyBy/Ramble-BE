package com.flyby.ramble.report.dto;

import com.flyby.ramble.report.model.BanReason;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BanUserCommandDTO {
    private Long userId;
    private BanReason banReason;
    private LocalDateTime bannedAt;
}
