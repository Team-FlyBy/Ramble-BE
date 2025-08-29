package com.flyby.ramble.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // TODO: JWT_BLACKLIST는 Redis로 변경
    JWT_BLACKLIST("jwtBlacklist", 30, 10000), // 30분, 최대 10,000개
    IP_REGION("ipRegion", 720, 20000),        // 720분(12시간), 최대 20,000개
    USER_INFO("user", 60, 10000);             // 60분, 최대 10,000개

    private final String cacheName;
    private final int expireAfterWrite; // 분(minutes) 단위
    private final int maximumSize;
}
