package com.flyby.ramble.report.model;

public enum UserReportStatus {
    PENDING,    // 신고가 접수되었으나 아직 처리되지 않음
    RESOLVED,   // 신고가 검토되어 처리됨 (예: 밴, 경고 등 조치 완료)
    REJECTED    // 신고가 검토되었으나 조치가 필요하지 않다고 판단됨
}
