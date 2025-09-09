package com.flyby.ramble.matching.model;

import lombok.*;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;

import java.time.LocalDateTime;

@Getter
@Builder
@REntity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingSessionInfo {

    @RId
    private String sessionId;

    @RIndex
    private String participantAId;

    @RIndex
    private String participantBId;

    private LocalDateTime startedAt;

}
