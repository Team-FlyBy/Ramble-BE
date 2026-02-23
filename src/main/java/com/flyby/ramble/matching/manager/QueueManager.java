package com.flyby.ramble.matching.manager;

import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchingProfile;
import com.flyby.ramble.matching.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueManager {
    private final RedissonClient redissonClient;

    private static final double CUT_OFF_TIME_MS = MatchingConstants.QUEUE_TTL * 60d * 1000;

    public boolean enqueue(MatchingProfile profile) {
        if (profile == null) {
            return false;
        }

        String queueKey   = RedisKeyBuilder.buildQueueKey(profile);
        String profileKey = RedisKeyBuilder.buildProfileKey(profile);
        String userId = profile.getUserExternalId();

        long now = System.currentTimeMillis();
        profile.setQueueEntryTime(now);

        // RBatch를 사용하여 SortedSet(대기열), Bucket(매칭 상세정보), SetCache(활성 대기열 키) 한 번에 처리
        RBatch batch = redissonClient.createBatch();

        // 대기열에 사용자 추가
        batch.getScoredSortedSet(queueKey, StringCodec.INSTANCE).addAsync(now, userId);
        batch.getBucket(profileKey)
                .setAsync(profile, Duration.ofMinutes(MatchingConstants.QUEUE_TTL));

        // 활성 대기열 키 추가
        batch.getSetCache(MatchingConstants.QUEUE_ACTIVE)
                .addAsync(queueKey, MatchingConstants.QUEUE_TTL, TimeUnit.MINUTES);

        try {
            batch.execute();
            return true;
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 재등록시 사용. Bucket 제외 큐에만 삽입 (TTL 유지)
     */
    public void requeueAll(Collection<MatchingProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }

        // RBatch를 사용하여 SortedSet(대기열) 한 번에 처리
        RBatch batch = redissonClient.createBatch();

        for (MatchingProfile profile : profiles) {
            String queueKey = RedisKeyBuilder.buildQueueKey(profile);

            long score = profile.getQueueEntryTime();
            // 대기열에 사용자 추가
            batch.getScoredSortedSet(queueKey, StringCodec.INSTANCE).addAsync(score, profile.getUserExternalId());
            // 활성 대기열 키 추가 (TTL에 의해 제거되었을 수 있어 재삽입)
            batch.getSetCache(MatchingConstants.QUEUE_ACTIVE)
                    .addAsync(queueKey, MatchingConstants.QUEUE_TTL, TimeUnit.MINUTES);
        }

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean dequeue(String userId) {
        MatchingProfile profile = getProfile(userId);

        return dequeue(profile);
    }

    public boolean dequeue(MatchingProfile profile) {
        if (profile == null) {
            return false;
        }

        String queueKey   = RedisKeyBuilder.buildQueueKey(profile);
        String profileKey = RedisKeyBuilder.buildProfileKey(profile);
        String userId = profile.getUserExternalId();

        // RBatch를 사용하여 SortedSet(대기열), Bucket(매칭 상세정보) 한 번에 처리
        RBatch batch = redissonClient.createBatch();

        // 대기열에서 사용자 제거
        RFuture<Boolean> removeQueue = batch.getScoredSortedSet(queueKey, StringCodec.INSTANCE).removeAsync(userId);
        RFuture<Boolean> removeProfile = batch.getBucket(profileKey).deleteAsync();

        // SetCache 비활성화는 TTL에 의해 처리 (대기열에 새로운 사용자가 없을시 TTL이 갱신되지 않아 자연스럽게 소멸)
        // SetCache는 있고 실제 대기열에 데이터가 없어도 문제 없음

        try {
            batch.execute();

            // 결과 조회
            boolean r1 = removeQueue.toCompletableFuture().join();
            boolean r2 = removeProfile.toCompletableFuture().join();

            return r1 && r2;
        } catch (Exception e) {
            log.error("Failed to remove user from queue: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 매칭 완료된 유저 정보 삭제
     */
    public void deleteProfiles(Set<String> userIds) {
        Set<String> profileKeys = userIds.stream()
                .map(RedisKeyBuilder::buildProfileKey)
                .collect(Collectors.toSet());

        executeDeleteBuckets(profileKeys); // 대기열 조회 시 poll 방식이라 Bucket만 삭제
    }

    public MatchingProfile getProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        String profileKey = RedisKeyBuilder.buildProfileKey(userId);

        RBucket<MatchingProfile> bucket = redissonClient.getBucket(profileKey);
        return bucket.get();
    }

    public Map<String, MatchingProfile> getProfiles(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> profileKeys = userIds.stream()
                .map(RedisKeyBuilder::buildProfileKey)
                .collect(Collectors.toSet());

        Map<String, MatchingProfile> raw = executeFetchBuckets(profileKeys);
        String prefix = MatchingConstants.PROFILE + ":";

        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(prefix.length()),
                        Map.Entry::getValue
                ));
    }

    /**
     * 모든 활성 대기열의 상태 조회
     */
    public Map<String, Integer> getActiveQueueSizes() {
        // 활성 대기열 조회
        RSet<String> activeQueues = redissonClient.getSetCache(MatchingConstants.QUEUE_ACTIVE);
        Set<String> keys = activeQueues.readAll();

        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        // RBatch를 사용하여 SortedSet(대기열) 여러 개를 한 번에 처리
        RBatch batch = redissonClient.createBatch();
        Map<String, RFuture<Integer>> futures = new LinkedHashMap<>();

        // 각 대기열의 크기 조회
        for (String queueKey : keys) {
            RScoredSortedSetAsync<String> queue = batch.getScoredSortedSet(queueKey);
            futures.put(queueKey, queue.sizeAsync());
        }

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyMap();
        }

        // 결과 매핑 및 반환
        return futures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toCompletableFuture().join(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * 대기열 조회 (모든 조건 일치: 성별, 언어, 지역)
     */
    public Map<String, List<String>> poll() {
        Map<String, Integer> activeKey = getActiveQueueSizes();

        return poll(activeKey);
    }

    /**
     * 대기열 조회 (queueKeys에 해당하는 대기열)
     * @param queueKeys map의 key(queueKey), value(size)
     */
    public Map<String, List<String>> poll(Map<String, Integer> queueKeys) {
        if (queueKeys == null || queueKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        // 각 대기열에서 조회한 결과를 반환
        return executePoll(
                queueKeys,
                Function.identity()
        );
    }

    /**
     * 모든 활성 대기열에서 대기열 키별 {@link MatchingProfile} 목록을 poll
     * <p>
     *     {@link #poll()}로 조회한 대기열을 기반으로
     *     {@link #getProfiles(Set)}로 프로필 정보를 조회하고,
     *     {@link MatchingProfile}로 매핑.
     * </p>
     * <b>NOTE:</b> 프로필이 만료되어 조회되지 않는 사용자는 결과에서 제외
     */
    public Map<String, List<MatchingProfile>> pollWithProfiles() {
        Map<String, List<String>> groups = poll(); // 대기열 조회
        Map<String, MatchingProfile> profiles = getProfiles(buildUserIds(groups)); // 프로필 조회

        if (groups.isEmpty() || profiles.isEmpty()) {
            return Collections.emptyMap();
        }

        // userId → MatchingProfile 변환, null 프로필 필터링
        Map<String, List<MatchingProfile>> result = new LinkedHashMap<>();

        groups.forEach((key, userIds) -> {
            List<MatchingProfile> profileList = userIds.stream()
                    .map(profiles::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedList::new));

            if (!profileList.isEmpty()) {
                result.put(key, profileList);
            }
        });

        return result;
    }

    /* --- 내부 메서드 --- */

    /**
     * Bucket 일괄 삭제
     * @param keys 삭제할 bucket key Set
     */
    private void executeDeleteBuckets(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // RBatch를 사용하여 Bucket 여러 개를 한 번에 처리
        RBatch batch = redissonClient.createBatch();

        // 각 Bucket을 제거
        for (String key : keys) {
            batch.getBucket(key).deleteAsync();
        }

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Bucket 일괄 조회
     * @param keys 조회할 bucket key Set
     */
    private <V> Map<String, V> executeFetchBuckets(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        RBuckets buckets = redissonClient.getBuckets();
        Map<String, V> result = buckets.get(keys.toArray(new String[0]));

        return result != null ? result : Collections.emptyMap();
    }

    /**
     * RScoredSortedSet 일괄 조회 + 만료된 항목 제거 (RScoredSortedSet은 개별 TTL 지원이 없음)
     * @param keys 조회할 key Map(key, size)
     */
    private <T, D> Map<String, List<D>> executePoll(
            Map<String, Integer> keys,
            Function<T, D> mapper) {
        // queueKeys 검증 및 전체 대기열(Backlog) 총합 계산
        long totalBacklog = calculateTotalSizes(keys);

        if (totalBacklog <= 0) {
            return Collections.emptyMap();
        }

        // RBatch를 사용하여 SortedSet(대기열) 여러 개를 한 번에 처리
        RBatch batch = redissonClient.createBatch();
        Map<String, RFuture<Collection<T>>> futures = new LinkedHashMap<>();
        double cutOffTime = System.currentTimeMillis() - CUT_OFF_TIME_MS; // 현재 시간 - 5분

        // 만료된 항목 제거
        for (String key : keys.keySet()) {
            batch.getScoredSortedSet(key, StringCodec.INSTANCE)
                    .removeRangeByScoreAsync(0, true, cutOffTime, true);
        }

        // 각 대기열에서 사용자를 poll
        keys.forEach((key, value) -> {
            int countToFetch = calculateProportionalSize(value, totalBacklog);

            RScoredSortedSetAsync<T> set = batch.getScoredSortedSet(key, StringCodec.INSTANCE);
            futures.put(key, set.pollFirstAsync(countToFetch));
        });

        // SetCache 비활성화는 TTL에 의해 처리 (대기열에 새로운 사용자가 없을시 TTL이 갱신되지 않아 자연스럽게 소멸)
        // SetCache는 있고 실제 대기열에 데이터가 없어도 문제 없음

        try {
            batch.execute();
        } catch (RedisException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyMap();
        }

        // 결과 매핑 및 반환
        return futures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Collection<T> values = entry.getValue().toCompletableFuture().join();

                            return values.stream()
                                    .map(mapper)
                                    .collect(Collectors.toCollection(LinkedList::new));
                        },
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private long calculateTotalSizes(Map<String, Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            return -1;
        }

        // keys 총합 계산
        return keys.values().stream()
                .mapToLong(Integer::longValue)
                .sum();
    }

    private int calculateProportionalSize(int value, long totalSize) {
        if (totalSize <= MatchingConstants.REDIS_BATCH_SIZE) {
            // Case A: 전체 데이터가 제한보다 적으면 있는 그대로
            return value;
        } else {
            // Case B: 전체 데이터가 제한보다 많으면 비례해서 -> (현재 큐크기 / 전체 크기) * 최대제한
            double ratio = (double) value / totalSize;
            return (int) Math.ceil(ratio * MatchingConstants.REDIS_BATCH_SIZE);
        }
    }

    private Set<String> buildUserIds(Map<String, List<String>> groups) {
        return groups.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

}
