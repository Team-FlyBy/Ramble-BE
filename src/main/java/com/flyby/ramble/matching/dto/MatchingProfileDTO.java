package com.flyby.ramble.matching.dto;

import com.flyby.ramble.user.model.Gender;
import lombok.*;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingProfileDTO implements Serializable {
    private Long id;
    private String externalId;

    private String region; // auto-detected
    private Gender gender; // auto-detected
    private String language;

    private String selectedRegion; // user-selected
    private Gender selectedGender; // user-selected

    @Setter
    private long queueEntryTime;
}
