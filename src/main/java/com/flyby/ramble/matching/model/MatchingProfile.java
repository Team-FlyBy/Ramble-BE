package com.flyby.ramble.matching.model;

import com.flyby.ramble.user.model.Gender;
import lombok.*;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;

@Getter
@Builder
@REntity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingProfile {

    @RId
    private String userExternalId;

    @RIndex
    private Long userId;

    private String region; // auto-detected
    private Gender gender; // auto-detected
    private String language;

    private String selectedRegion; // user-selected
    private Gender selectedGender; // user-selected

    @Setter
    private long queueEntryTime;
}
