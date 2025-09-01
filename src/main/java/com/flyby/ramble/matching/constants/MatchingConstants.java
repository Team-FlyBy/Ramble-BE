package com.flyby.ramble.matching.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MatchingConstants {

    /*  WebSocket Subscription Keys  */

    public static final String SUBSCRIPTION_MATCHING  = "/queue/match";
    public static final String SUBSCRIPTION_SIGNALING = "/queue/signal";

    /*  Redis Keys  */

    public static final String WAITING_QUEUE_KEY    = "webrtc:waiting_queue";
    public static final String USER_PROFILE_KEY     = "webrtc:user_profile";
    public static final String CHAT_SESSION_KEY     = "webrtc:chat_session";
    public static final String MATCHMAKING_LOCK_KEY = "webrtc:matchmaking_lock"; // 분산 락을 위한 키

    /*  Other Constants  */

    public static final int WAITING_SCORE_WEIGHT_PER_10_SECONDS = 1;
    public static final int MINIMUM_MATCH_SCORE = 10;

}
