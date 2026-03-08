package com.flyby.ramble.matching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Language {
    KO, // 한국어
    EN, // 영어
    HI, // 힌디어
    ID, // 인도네시아어
    TL, // 타갈로그어
    PT, // 포르투갈어
    AR, // 아랍어
    FR, // 프랑스어
    ES, // 스페인어
    TR, // 튀르키예어
    DE, // 독일어
    DA, // 덴마크어
    VI, // 베트남어
    NONE;

    @JsonCreator
    public static Language from(String value) {
        return Stream.of(Language.values())
                .filter(language -> language.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("알 수 없는 언어 코드, NONE으로 처리: {}", value);
                    return NONE;
                });
    }

}
