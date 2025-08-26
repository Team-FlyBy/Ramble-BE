package com.flyby.ramble.report.model;

import lombok.Getter;

@Getter
public enum BanReason {
    REPORT_ACCUMULATION("신고 누적 정지"),
    MANUAL_ADMIN_ACTION("관리자 수동 정지"),
    AUTO_NUDE_DETECTION("자동 누드 감지 정지"),
    NUDE_DETECTION("누드 감지 정지");

    private final String description;

    BanReason(String description) {
        this.description = description;
    }
}
