package com.flyby.ramble.matching.manager;

import com.flyby.ramble.matching.RedisTestBase;
import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.dto.MatchingProfile;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.util.RedisKeyBuilder;
import com.flyby.ramble.user.model.Gender;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueManager 테스트 (실제 Redis)")
@ContextConfiguration(classes = QueueManager.class)
class QueueManagerTest extends RedisTestBase {

    @Autowired
    private QueueManager queueManager;

    private MatchingProfile testProfile1;
    private MatchingProfile testProfile2;
    private MatchingProfile testProfile3;

    @BeforeEach
    void setUp() {
        testProfile1 = MatchingProfile.builder()
                .userId(1L)
                .userExternalId("user-1")
                .region(Region.KR)
                .gender(Gender.MALE)
                .language(Language.KO)
                .build();

        testProfile2 = MatchingProfile.builder()
                .userId(2L)
                .userExternalId("user-2")
                .region(Region.KR)
                .gender(Gender.MALE)
                .language(Language.KO)
                .build();

        testProfile3 = MatchingProfile.builder()
                .userId(3L)
                .userExternalId("user-3")
                .region(Region.US)
                .gender(Gender.FEMALE)
                .language(Language.EN)
                .build();
    }

    @DisplayName("enqueue: 프로필 저장 및 대기열 추가")
    @Test
    @Order(1)
    void enqueue_savesProfileAndQueue() {
        // when
        queueManager.enqueue(testProfile1);
        String queueKey = RedisKeyBuilder.buildQueueKey(testProfile1);

        // then - 프로필 및 대기열이 저장되었는지 확인
        MatchingProfile retrieved = queueManager.getProfile("user-1");
        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey, StringCodec.INSTANCE);
        String userId = queue.first();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUserExternalId()).isEqualTo("user-1");
        assertThat(retrieved.getUserId()).isEqualTo(1L);
        assertThat(retrieved.getRegion()).isEqualTo(Region.KR);
        assertThat(retrieved.getGender()).isEqualTo(Gender.MALE);
        assertThat(retrieved.getLanguage()).isEqualTo(Language.KO);
        assertThat(retrieved.getQueueEntryTime()).isGreaterThan(0);
        assertThat(userId).isEqualTo("user-1");
    }

    @DisplayName("requeue: 대기열에만 추가 (프로필 저장 안함)")
    @Test
    @Order(2)
    void requeue_addsToQueueOnly() {
        // when
        queueManager.requeueAll(List.of(testProfile1));
        String queueKey = RedisKeyBuilder.buildQueueKey(testProfile1);

        // then
        MatchingProfile retrieved = queueManager.getProfile("user-1");
        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey, StringCodec.INSTANCE);
        String userId = queue.first();

        assertThat(retrieved).isNull();
        assertThat(userId).isEqualTo("user-1");
    }

    @DisplayName("enqueue: 다수 사용자 추가")
    @Test
    @Order(3)
    void enqueue_multipleUsers() {
        // when
        queueManager.enqueue(testProfile1);
        queueManager.enqueue(testProfile2);
        queueManager.enqueue(testProfile3);

        // then - 모든 프로필이 저장되었는지 확인
        Map<String, MatchingProfile> profiles = queueManager.getProfiles(
                Set.of("user-1", "user-2", "user-3")
        );

        assertThat(profiles).hasSize(3);
        assertThat(profiles.values())
                .extracting(MatchingProfile::getUserExternalId)
                .containsExactlyInAnyOrder("user-1", "user-2", "user-3");
    }

    @DisplayName("dequeue: Profile로 제거")
    @Test
    @Order(4)
    void dequeue_byProfile() {
        // given
        queueManager.enqueue(testProfile1);
        assertThat(queueManager.getProfile("user-1")).isNotNull();

        // when
        boolean removed = queueManager.dequeue(testProfile1);

        // then
        assertThat(removed).isTrue();
        assertThat(queueManager.getProfile("user-1")).isNull();
    }

    @DisplayName("dequeue: userId로 제거")
    @Test
    @Order(5)
    void dequeue_byUserId() {
        // given
        queueManager.enqueue(testProfile1);
        assertThat(queueManager.getProfile("user-1")).isNotNull();

        // when
        boolean removed = queueManager.dequeue("user-1");

        // then
        assertThat(removed).isTrue();
        assertThat(queueManager.getProfile("user-1")).isNull();
    }

    @DisplayName("deleteProfiles: 일괄 제거")
    @Test
    @Order(6)
    void deleteProfiles_batch() {
        // given
        queueManager.enqueue(testProfile1);
        queueManager.enqueue(testProfile2);
        queueManager.enqueue(testProfile3);

        // when
        queueManager.deleteProfiles(Set.of("user-1", "user-2"));

        // then
        assertThat(queueManager.getProfile("user-1")).isNull();
        assertThat(queueManager.getProfile("user-2")).isNull();
        assertThat(queueManager.getProfile("user-3")).isNotNull();
    }

    @DisplayName("dequeue: 존재하지 않는 사용자 → false")
    @Test
    @Order(7)
    void dequeue_nonExistent_returnsFalse() {
        // when
        boolean removed = queueManager.dequeue("non-existent-user");

        // then
        assertThat(removed).isFalse();
    }

    @DisplayName("enqueue: TTL 5분 확인")
    @Test
    @Order(8)
    void enqueue_checkTTL() {
        // given
        queueManager.enqueue(testProfile1);
        String profileKey = RedisKeyBuilder.buildProfileKey(testProfile1);

        // when - TTL 확인 (밀리초 단위)
        long ttl = redissonClient.getBucket(profileKey).remainTimeToLive();

        // then - TTL이 약 5분(300초 = 300,000ms)이어야 함
        assertThat(ttl).isBetween(299_000L, 300_000L);
    }

    @DisplayName("enqueue: 같은 큐 - 순서 유지 확인")
    @Test
    @Order(9)
    void enqueue_sameQueue_maintainsOrder() throws InterruptedException {
        // given - 동일한 성별/언어/지역의 사용자들
        queueManager.enqueue(testProfile1);
        Thread.sleep(10); // 시간 차이를 두기 위해
        queueManager.enqueue(testProfile2);

        // when - 큐 키 확인
        String queueKey = RedisKeyBuilder.buildQueueKey(Gender.MALE, Language.KO, Region.KR);

        Map<String, List<String>> groups = queueManager.poll();
        List<String> queue = groups.get(queueKey);

        // then - 두 사용자가 같은 큐에 있어야 함
        assertThat(queue).hasSize(2);

        // 추가된 순서로 반환되어야 함
        String user1 = queue.get(0);
        String user2 = queue.get(1);
        assertThat(user1).isEqualTo(testProfile1.getUserExternalId());
        assertThat(user2).isEqualTo(testProfile2.getUserExternalId());
    }

    @DisplayName("enqueue: 다른 큐 - 분리 확인")
    @Test
    @Order(10)
    void enqueue_differentQueues_separates() {
        // given
        queueManager.enqueue(testProfile1); // MALE, KO, KR
        queueManager.enqueue(testProfile3); // FEMALE, EN, US

        // when
        String queue1Key = "match:queue:MALE:KO:KR";
        String queue2Key = "match:queue:FEMALE:EN:US";

        Map<String, List<String>> groups = queueManager.poll();
        List<String> queue1 = groups.get(queue1Key);
        List<String> queue2 = groups.get(queue2Key);

        // then
        assertThat(queue1).hasSize(1);
        assertThat(queue2).hasSize(1);
        assertThat(queue1).contains("user-1");
        assertThat(queue2).contains("user-3");
    }

    @DisplayName("getProfiles: 빈 Set → 빈 Map")
    @Test
    @Order(11)
    void getProfiles_emptySet_returnsEmpty() {
        // when
        Map<String, MatchingProfile> profiles = queueManager.getProfiles(Set.of());

        // then
        assertThat(profiles).isEmpty();
    }

    @DisplayName("dequeue: null → false")
    @Test
    @Order(12)
    void dequeue_null_returnsFalse() {
        // when
        boolean removed = queueManager.dequeue((MatchingProfile) null);

        // then
        assertThat(removed).isFalse();
    }

    @DisplayName("[성능] 대량 처리 - 같은 큐")
    @Test
    @Order(13)
    void bulk_sameQueue_performance() {
        // given - 테스트 사용자 생성
        int userCount = 1000; // 변경하면서 테스트
        Set<String> userIds = new HashSet<>();

        System.out.println("시작: " + LocalDateTime.now());
        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(Region.KR)
                    .gender(Gender.MALE)
                    .language(Language.KO)
                    .build();

            userIds.add("user-" + i);
            queueManager.enqueue(profile);
        }
        System.out.println("삽입 완료: " + LocalDateTime.now());

        // when (1) - 모든 사용자 프로필 조회
        Map<String, MatchingProfile> profiles = queueManager.getProfiles(userIds);
        System.out.println("프로필 조회 완료: " + LocalDateTime.now());
        Map<String, List<String>> queues = queueManager.poll();
        System.out.println("대기열 조회 완료: " + LocalDateTime.now());

        List<String> queue = queues.get("match:queue:MALE:KO:KR");

        // then - 조회 검증
        assertThat(profiles).hasSize(userCount);
        assertThat(queue).isNotEmpty(); //.hasSize(Math.min(userCount, 50)); // pollWithExactMatch는 큐마다 최대 50개씩

        // when (2) - 일괄 삭제
        queueManager.deleteProfiles(userIds);
        System.out.println("삭제 완료: " + LocalDateTime.now());

        // 삭제 확인
        Map<String, MatchingProfile> afterRemoval = queueManager.getProfiles(userIds);
        assertThat(afterRemoval).isEmpty();
    }

    @DisplayName("[성능] 대량 처리 - 다른 큐")
    @Test
    @Order(14)
    void bulk_differentQueues_performance() {
        Gender[] genders = Gender.values();
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given - 테스트 사용자 생성
        int userCount = 1000; // 변경하면서 테스트
        Set<String> userIds = new HashSet<>();

        System.out.println("시작: " + LocalDateTime.now());
        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(regions[i % (regions.length-1)])
                    .gender(genders[i % (genders.length-1)])
                    .language(languages[i % (languages.length-1)])
                    .build();

            userIds.add("user-" + i);
            queueManager.enqueue(profile);
        }
        System.out.println("삽입 완료: " + LocalDateTime.now());

        // when (1) - 모든 사용자 프로필 조회
        Map<String, MatchingProfile> profiles = queueManager.getProfiles(userIds);
        System.out.println("프로필 조회 완료: " + LocalDateTime.now());
        Map<String, List<String>> queues = queueManager.poll();
        System.out.println("대기열 조회 완료: " + LocalDateTime.now());

        // then - 조회 검증
        assertThat(profiles).hasSize(userCount);
        assertThat(queues).isNotEmpty().hasSize(Math.min(userCount, 390)); // 390은 최대 조합 수
        assertThat(queues.values())
                .flatExtracting(list -> list) // 각 리스트의 원소들을 하나로 합침
                .hasSize(Math.min(userCount, MatchingConstants.REDIS_BATCH_SIZE)) // queueManager의 poll 최대 개수 (REDIS_BATCH_SIZE와 동일하지는 않음)
                .doesNotHaveDuplicates();

        // when (2) - 일괄 삭제
        queueManager.deleteProfiles(userIds);
        System.out.println("삭제 완료: " + LocalDateTime.now());

        // then - 삭제 확인
        Map<String, MatchingProfile> afterRemoval = queueManager.getProfiles(userIds);
        assertThat(afterRemoval).isEmpty();
    }

    @DisplayName("[동시성] 멀티스레드 enqueue - 같은 큐")
    @Test
    @Order(15)
    void enqueue_concurrent_sameQueue() {
        // given
        int threadCount = 500; // 변경하면서 테스트
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when - 스레드에서 동시에 사용자 추가
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    MatchingProfile profile = MatchingProfile.builder()
                            .userId((long) i)
                            .userExternalId("concurrent-user-" + i)
                            .region(Region.KR)
                            .gender(Gender.MALE)
                            .language(Language.KO)
                            .build();
                    queueManager.enqueue(profile);
                }, executor))
                .toList();

        // 모든 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then - 모든 사용자가 정상적으로 추가되었는지 확인
        String queueKey = RedisKeyBuilder.buildQueueKey(Gender.MALE, Language.KO, Region.KR);
        Map<String, List<String>> groups = queueManager.poll();
        List<String> queue = groups.get(queueKey);

        // queueManager의 poll 최대 개수 (엄밀히는 REDIS_BATCH_SIZE와 동일하지는 않음, calculateProportionalSize 때문)
        assertThat(queue).hasSize(Math.min(threadCount, MatchingConstants.REDIS_BATCH_SIZE));
    }

    @DisplayName("[동시성] 멀티스레드 enqueue - 다른 큐")
    @Test
    @Order(16)
    void enqueue_concurrent_differentQueues() {
        Gender[] genders = Gender.values();
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given
        int threadCount = 500; // 변경하면서 테스트
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when - 스레드에서 동시에 사용자 추가
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(index -> CompletableFuture.runAsync(() -> {
                    MatchingProfile profile = MatchingProfile.builder()
                            .userId((long) index)
                            .userExternalId("concurrent-user-" + index)
                            .region(regions[index % (regions.length-1)])
                            .gender(genders[index % (genders.length-1)])
                            .language(languages[index % (languages.length-1)])
                            .build();
                    queueManager.enqueue(profile);
                }, executor))
                .toList();

        // 모든 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then - 모든 사용자가 정상적으로 추가되었는지 확인
        Map<String, List<String>> queues = queueManager.poll();

        // queueManager의 poll 최대 개수 (엄밀히는 REDIS_BATCH_SIZE와 동일하지는 않음, calculateProportionalSize 때문)
        int minExpectedSize = Math.min(threadCount, MatchingConstants.REDIS_BATCH_SIZE) / 390; // (390은 최대 조합 수)
        int maxExpectedSize = minExpectedSize + 1;

        assertThat(queues).isNotEmpty().hasSize(Math.min(threadCount, 390)); // 390은 최대 조합 수
        // 각 대기열 사이즈 검증
        assertThat(queues.values()).allSatisfy(userList ->
                assertThat(userList.size()).isBetween(minExpectedSize, maxExpectedSize));
    }

    @DisplayName("[대량] pollWithProfiles - 같은 큐")
    @Test
    @Order(17)
    void pollWithProfiles_bulk_sameQueue() {
        // given
        int userCount = 1000;

        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(Region.KR)
                    .gender(Gender.MALE)
                    .language(Language.KO)
                    .build();
            queueManager.enqueue(profile);
        }

        // when
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();

        // then
        String queueKey = RedisKeyBuilder.buildQueueKey(Gender.MALE, Language.KO, Region.KR);

        assertThat(result).hasSize(1).containsKey(queueKey);

        List<MatchingProfile> profiles = result.get(queueKey);
        assertThat(profiles)
                .hasSize(Math.min(userCount, MatchingConstants.REDIS_BATCH_SIZE))
                .allSatisfy(p -> {
                    assertThat(p.getUserExternalId()).startsWith("user-");
                    assertThat(p.getRegion()).isEqualTo(Region.KR);
                    assertThat(p.getGender()).isEqualTo(Gender.MALE);
                    assertThat(p.getLanguage()).isEqualTo(Language.KO);
                    assertThat(p.getQueueEntryTime()).isGreaterThan(0);
                })
                .doesNotHaveDuplicates();
    }

    @DisplayName("[대량] pollWithProfiles - 다른 큐")
    @Test
    @Order(18)
    void pollWithProfiles_bulk_differentQueues() {
        Gender[] genders = Gender.values();
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given
        int userCount = 1000;

        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(regions[i % (regions.length - 1)])
                    .gender(genders[i % (genders.length - 1)])
                    .language(languages[i % (languages.length - 1)])
                    .build();
            queueManager.enqueue(profile);
        }

        // when
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();

        // then - 여러 큐에 분산되어야 함
        assertThat(result).hasSizeGreaterThan(1);

        // 모든 프로필이 유효해야 함
        List<MatchingProfile> allProfiles = result.values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(allProfiles)
                .hasSizeLessThanOrEqualTo(MatchingConstants.REDIS_BATCH_SIZE)
                .allSatisfy(p -> {
                    assertThat(p.getUserExternalId()).startsWith("user-");
                    assertThat(p.getQueueEntryTime()).isGreaterThan(0);
                })
                .doesNotHaveDuplicates();

        // 각 큐 내 프로필의 속성이 일관성 있어야 함 (같은 큐 키 → 같은 gender/language/region)
        result.forEach((key, profiles) -> {
            Set<String> queueKeys = profiles.stream()
                    .map(RedisKeyBuilder::buildQueueKey)
                    .collect(Collectors.toSet());
            assertThat(queueKeys).as("큐 '%s'의 프로필은 모두 같은 큐 키를 가져야 함", key).hasSize(1);
        });
    }

    @DisplayName("[성능] pollWithProfiles - 같은 큐")
    @Test
    @Order(19)
    void pollWithProfiles_performance_sameQueue() {
        // given
        int userCount = 1000;

        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(Region.KR)
                    .gender(Gender.MALE)
                    .language(Language.KO)
                    .build();
            queueManager.enqueue(profile);
        }

        // when
        LocalDateTime start = LocalDateTime.now();
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();
        LocalDateTime end   = LocalDateTime.now();

        // then
        long elapsed = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("소요 시간: " + elapsed + "ms");

        assertThat(result).isNotEmpty();
        assertThat(elapsed).as("pollWithProfiles 같은 큐 %dms 이내", 500).isLessThan(500);
    }

    @DisplayName("[성능] pollWithProfiles - 다른 큐")
    @Test
    @Order(20)
    void pollWithProfiles_performance_differentQueues() {
        Gender[] genders = Gender.values();
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given
        int userCount = 1000;

        for (int i = 0; i < userCount; i++) {
            MatchingProfile profile = MatchingProfile.builder()
                    .userId((long) i)
                    .userExternalId("user-" + i)
                    .region(regions[i % (regions.length - 1)])
                    .gender(genders[i % (genders.length - 1)])
                    .language(languages[i % (languages.length - 1)])
                    .build();
            queueManager.enqueue(profile);
        }

        // when
        LocalDateTime start = LocalDateTime.now();
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();
        LocalDateTime end   = LocalDateTime.now();

        // then
        long elapsed = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("소요 시간: " + elapsed + "ms");
        System.out.println("[성능] pollWithProfiles 다른 큐 (" + userCount + "명): " + elapsed + "ms");

        assertThat(result).isNotEmpty();
        assertThat(elapsed).as("pollWithProfiles 다른 큐 %dms 이내", 500).isLessThan(500);
    }

    @DisplayName("[동시성] pollWithProfiles - 같은 큐")
    @Test
    @Order(21)
    void pollWithProfiles_concurrent_sameQueue() {
        // given - 멀티스레드로 동일 큐에 enqueue
        int threadCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    MatchingProfile profile = MatchingProfile.builder()
                            .userId((long) i)
                            .userExternalId("concurrent-user-" + i)
                            .region(Region.KR)
                            .gender(Gender.MALE)
                            .language(Language.KO)
                            .build();
                    queueManager.enqueue(profile);
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // when
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();

        // then
        String queueKey = RedisKeyBuilder.buildQueueKey(Gender.MALE, Language.KO, Region.KR);

        assertThat(result).hasSize(1).containsKey(queueKey);

        List<MatchingProfile> profiles = result.get(queueKey);
        assertThat(profiles)
                .hasSize(Math.min(threadCount, MatchingConstants.REDIS_BATCH_SIZE))
                .allSatisfy(p -> {
                    assertThat(p.getUserExternalId()).startsWith("concurrent-user-");
                    assertThat(p.getRegion()).isEqualTo(Region.KR);
                    assertThat(p.getGender()).isEqualTo(Gender.MALE);
                    assertThat(p.getLanguage()).isEqualTo(Language.KO);
                })
                .doesNotHaveDuplicates();
    }

    @DisplayName("[동시성] pollWithProfiles - 다른 큐")
    @Test
    @Order(22)
    void pollWithProfiles_concurrent_differentQueues() {
        Gender[] genders = Gender.values();
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given - 멀티스레드로 다양한 큐에 enqueue
        int threadCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    MatchingProfile profile = MatchingProfile.builder()
                            .userId((long) i)
                            .userExternalId("concurrent-user-" + i)
                            .region(regions[i % (regions.length - 1)])
                            .gender(genders[i % (genders.length - 1)])
                            .language(languages[i % (languages.length - 1)])
                            .build();
                    queueManager.enqueue(profile);
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // when
        Map<String, List<MatchingProfile>> result = queueManager.pollWithProfiles();

        // then - 여러 큐에 분산
        assertThat(result).hasSizeGreaterThan(1);

        List<MatchingProfile> allProfiles = result.values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(allProfiles)
                .hasSizeLessThanOrEqualTo(MatchingConstants.REDIS_BATCH_SIZE)
                .allSatisfy(p -> assertThat(p.getUserExternalId()).startsWith("concurrent-user-"))
                .doesNotHaveDuplicates();

        // 각 큐 내 프로필 속성 일관성 검증
        result.forEach((key, profiles) -> {
            Set<String> queueKeys = profiles.stream()
                    .map(RedisKeyBuilder::buildQueueKey)
                    .collect(Collectors.toSet());
            assertThat(queueKeys).as("큐 '%s'의 프로필은 모두 같은 큐 키를 가져야 함", key).hasSize(1);
        });
    }

    @DisplayName("Redis 연결 확인")
    @Test
    @Order(23)
    void redis_connectionHealthCheck() {
        // when - 간단한 read/write로 연결 확인
        RBucket<String> bucket = redissonClient.getBucket("health:check");
        bucket.set("OK");
        String result = bucket.get();
        bucket.delete();

        // then
        assertThat(result).isEqualTo("OK");
    }
}