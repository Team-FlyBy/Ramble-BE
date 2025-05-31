package com.flyby.ramble.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    JWT_BLACKLIST("jwtBlacklist");

    private final String value;

    /**
     * 모든 캐시 이름을 반환
     *
     * @return 캐시 이름 목록 (불변 리스트)
     */
    public static List<String> names() {
        return Arrays.stream(CacheType.values())
                .map(CacheType::getValue)
                .toList();
    }

}
