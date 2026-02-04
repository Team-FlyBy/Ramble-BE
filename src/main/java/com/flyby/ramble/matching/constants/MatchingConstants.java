package com.flyby.ramble.matching.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MatchingConstants {

    /*  WebSocket Subscription Keys  */

    public static final String SUBSCRIPTION_MATCHING  = "/queue/match";
    public static final String SUBSCRIPTION_SIGNALING = "/queue/signal";

    /*  Redis */

    // 매칭 대기열
    public static final String QUEUE        = "match:queue";        // 매칭 대기열 키
    public static final String QUEUE_ACTIVE = "match:queue:active"; // 활성 대기열 키 (매칭 대기열 키 목록 반환)
    // 매칭 프로필
    public static final String PROFILE      = "match:profile";      // 매칭 프로필 키
    // 세션
    public static final String SESSION      = "match:session";      // 매칭 세션 키
    public static final String SESSION_USER = "match:session:user"; // userId → sessionId 매핑

    public static final int REDIS_BATCH_SIZE = 1000; // Redis 배치 처리 단위
    public static final int QUEUE_TTL = 5;           // 대기열 TTL (분)
    public static final int SESSION_TTL = 720;       // 세션 TTL (분, 12시간)

}
