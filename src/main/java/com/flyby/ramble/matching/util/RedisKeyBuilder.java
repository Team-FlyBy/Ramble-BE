package com.flyby.ramble.matching.util;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchingProfile;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.user.model.Gender;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKeyBuilder {

    /**
     * 대기열 키 생성 (MatchingProfile)
     */
    public String buildQueueKey(MatchingProfile profile) {
        return buildQueueKey(profile.getGender(), profile.getLanguage(), profile.getRegion());
    }

    /**
     * 대기열 키 생성 (성별:언어:지역)
     */
    public String buildQueueKey(Gender gender, Language language, Region region) {
        // "%s:%s:%s:%s" 형식
        return MatchingConstants.QUEUE + ":" + gender.name() + ":" + language.name() + ":" + region.name();
    }

    /**
     * 프로필 키 생성 (MatchingProfile)
     */
    public String buildProfileKey(MatchingProfile profile) {
        return MatchingConstants.PROFILE + ":" + profile.getUserExternalId();
    }

    /**
     * 프로필 키 생성 (userId)
     */
    public String buildProfileKey(String userId) {
        validateKey(userId);
        return MatchingConstants.PROFILE + ":" + userId;
    }

    public String buildSessionKey(String key) {
        validateKey(key);
        return MatchingConstants.SESSION + ":" + key;
    }

    public String buildSessionUserKey(String key) {
        validateKey(key);
        return MatchingConstants.SESSION_USER + ":" + key;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis Key는 null이거나 비어있을 수 없습니다");
        }

        // 공백 검증
        if (key.trim().length() != key.length()) {
            throw new IllegalArgumentException("Redis Key에 앞뒤 공백이 포함되어 있습니다: " + key);
        }

        // Redis 키에 사용할 수 없는 문자 검증
        if (!key.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Redis Key에 허용되지 않은 문자가 포함되어 있습니다: " + key);
        }

        // 최대 길이 제한
        if (key.length() > 100) {
            throw new IllegalArgumentException("Redis Key가 너무 깁니다: " + key.length());
        }
    }

}
