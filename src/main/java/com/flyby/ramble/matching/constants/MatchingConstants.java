package com.flyby.ramble.matching.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MatchingConstants {

    /*  WebSocket Subscription Keys  */

    public static final String SUBSCRIPTION_MATCHING  = "/queue/match";
    public static final String SUBSCRIPTION_SIGNALING = "/queue/signal";

    /*  Redis Keys  */
    public static final String MATCHMAKING_LOCK_KEY = "webrtc:matchmaking_lock"; // 분산 락을 위한 키
    public static final String MATCHMAKING_POOL_KEY = "webrtc:matchmaking_pool"; // 매칭 대기자 풀 키

    /*  Redis    */

    public static final int MAX_WAITING_QUEUE_SIZE = 50;
    public static final int LOCK_WAIT_SECONDS = 5;
    public static final int LOCK_LEASE_SECONDS = 10;

    /*  Scoring Weights  */

    public static final int WAITING_SCORE_PER_10_SECONDS = 1;
    public static final int MINIMUM_MATCH_SCORE = 10;

    public static final int REGION_MATCH_SCORE = 15;
    public static final int REGION_PARTIAL_MATCH_SCORE = 5;
    public static final int GENDER_DIFFERENCE_SCORE = 5;
    public static final int LANGUAGE_MATCH_SCORE = 5;

}
