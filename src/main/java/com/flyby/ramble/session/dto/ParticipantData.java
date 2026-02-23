package com.flyby.ramble.session.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flyby.ramble.matching.dto.MatchingProfile;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.user.model.Gender;

/**
 * Redis 참가자 데이터
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public record ParticipantData(
    Long userId,
    String userExternalId,
    Region region,
    Gender gender,
    Language language
) {
    public static ParticipantData from(MatchingProfile participant) {
        return new ParticipantData(
            participant.getUserId(),
            participant.getUserExternalId(),
            participant.getRegion(),
            participant.getGender(),
            participant.getLanguage()
        );
    }
}
