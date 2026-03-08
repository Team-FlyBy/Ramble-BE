package com.flyby.ramble.matching.dto;

import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.user.model.Gender;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingProfile {

    private Long userId;
    private String userExternalId;
    private Region region;
    private Gender gender;
    private Language language;

    @Setter
    private long queueEntryTime;

    @Builder
    public MatchingProfile(Long userId, String userExternalId, Region region, Gender gender, Language language, Region selectedRegion, Gender selectedGender){
        this.userId = userId;
        this.userExternalId = userExternalId;
        this.region = getEffectiveRegion(selectedRegion, region);
        this.gender = getEffectiveGender(selectedGender, gender);
        this.language = getEffectiveLanguage(language);
    }

    private Region getEffectiveRegion(Region selectedRegion, Region region) {
        if (isValid(selectedRegion)) {
            return selectedRegion;
        }

        if (isValid(region)) {
            return region;
        }

        return Region.US; // 기본 지역은 추후 변경될 수 있음
    }

    private Gender getEffectiveGender(Gender selectedGender, Gender gender) {
        if (isValid(selectedGender)) {
            return selectedGender;
        }

        if (isValid(gender)) {
            return gender;
        }

        return Gender.MALE;
    }

    private Language getEffectiveLanguage(Language language) {
        if (language != null && language != Language.NONE) {
            return language;
        }

        return Language.EN;
    }

    private boolean isValid(Region region) {
        return region != null && region != Region.NONE;
    }

    private boolean isValid(Gender gender) {
        return gender != null && gender != Gender.UNKNOWN;
    }
}
