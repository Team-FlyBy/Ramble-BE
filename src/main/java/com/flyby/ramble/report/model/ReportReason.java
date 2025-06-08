package com.flyby.ramble.report.model;

public enum ReportReason {
    // 음란 및 부적절 행위
    SEXUAL_CONTENT,              // 음란행위, 성적인 노출
    INAPPROPRIATE_BEHAVIOR,      // 비속어, 욕설, 부적절한 언행

    // 범죄 및 위험 행위
    HARASSMENT_OR_BULLYING,      // 괴롭힘, 협박
    FRAUD_OR_SCAM,               // 금전 요구, 사기 시도
    ILLEGAL_ACTIVITY,            // 불법행위 시도 (예: 약물, 도박 권유)

    // 시스템 악용 및 도촬
    ADVERTISEMENT_SPAM,          // 광고 목적 접속

    // 기타
    OTHER                        // 기타 (직접 입력)
}
