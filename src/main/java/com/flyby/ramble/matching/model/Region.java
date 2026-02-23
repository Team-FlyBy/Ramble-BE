package com.flyby.ramble.matching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Region {
    KR, // 한국
    US, // 미국
    GB, // 영국
    IN, // 인도
    ID, // 인도네시아
    PH, // 필리핀
    MA, // 모로코
    FR, // 프랑스
    ES, // 스페인
    TR, // 튀르키예
    BR, // 브라질
    DE, // 독일
    DK, // 덴마크
    MX, // 멕시코
    VN, // 베트남
    NONE;

    @JsonCreator
    public static Region from(String value) {
        return Stream.of(Region.values())
                .filter(region -> region.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("알 수 없는 지역 코드, NONE으로 처리: {}", value);
                    return NONE;
                });
    }
}
