package com.flyby.ramble.matching.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingSessionInfoDTO implements Serializable {
    UUID sessionId;
    String partnerInfo;
    LocalDateTime startedAt;
}
