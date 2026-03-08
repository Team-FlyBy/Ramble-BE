package com.flyby.ramble.session.repository;

import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.user.model.Gender;

/**
 * DB 배치 삽입용 참가자 데이터
 * SessionBatchRepository 내부에서만 사용
 */
record ParticipantBatchData(
    Long sessionId,
    Long userId,
    Region region,
    Gender gender,
    Language language
) {
    public static ParticipantBatchData from(ParticipantData data) {
        return new ParticipantBatchData(
            null,
            data.userId(),
            data.region(),
            data.gender(),
            data.language()
        );
    }

    public ParticipantBatchData withSessionId(Long sessionId) {
        return new ParticipantBatchData(sessionId, userId, region, gender, language);
    }
}
