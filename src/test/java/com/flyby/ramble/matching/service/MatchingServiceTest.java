package com.flyby.ramble.matching.service;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.matching.RedisTestBase;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.dto.MatchingProfile;
import com.flyby.ramble.matching.manager.QueueManager;
import com.flyby.ramble.matching.manager.SessionManager;
import com.flyby.ramble.matching.manager.SignalingRelayer;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.MatchStatus;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.util.RedisKeyBuilder;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.Role;
import com.flyby.ramble.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@DisplayName("MatchingService 테스트 (실제 Redis)")
@ContextConfiguration(classes = {
        QueueManager.class,
        SessionManager.class,
        MatchingService.class,
})
class MatchingServiceTest extends RedisTestBase {

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SignalingRelayer signalingRelayer;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MatchingService matchingService;

    private List<UserInfoDTO> testUsers;

    @BeforeEach
    void setUp() {
        testUsers = createUserList(1000);

        given(userService.getUserByExternalId(anyString()))
                .willAnswer(invocation -> {
                    String inputId = invocation.getArgument(0);
                    return testUsers.stream()
                            .filter(user -> user.getExternalId().equals(inputId))
                            .findFirst()
                            .orElseThrow();
                });
    }

    @DisplayName("requestMatch: 유효한 요청 → WAITING 반환 및 대기열 등록")
    @Test
    @Order(1)
    void requestMatch_validRequest_returnsWaiting() {
        // when
        MatchResultDTO result = enqueueUser(0, Language.KO, Region.KR);

        // then
        assertThat(result.status()).isEqualTo(MatchStatus.WAITING);

        MatchingProfile profile = queueManager.getProfile(getExternalId(0));
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();

        assertThat(profile).isNotNull();
        assertThat(profile.getLanguage()).isEqualTo(Language.KO);
        assertThat(profile.getRegion()).isEqualTo(Region.KR);
        assertThat(profile.getGender()).isEqualTo(Gender.MALE); // index 0 → MALE
        assertThat(sizes).hasSize(1);
    }

    @DisplayName("requestMatch: null 요청 → FAILED 반환")
    @Test
    @Order(2)
    void requestMatch_nullRequest_returnsFailed() {
        // when
        MatchResultDTO result = matchingService.requestMatch(getExternalId(0), Region.KR, null);

        // then
        assertThat(result.status()).isEqualTo(MatchStatus.FAILED);
        assertThat(result.message()).isNotNull();

        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        assertThat(sizes).isEmpty();
    }

    @DisplayName("requestMatch: 동일 유저 재요청 → 기존 데이터 정리 후 재등록")
    @Test
    @Order(3)
    void requestMatch_duplicateUser_cleansOldAndReenqueues() {
        // given - 첫 번째 등록 (KO, KR)
        enqueueUser(0, Language.KO, Region.KR);
        MatchingProfile firstProfile = queueManager.getProfile(getExternalId(0));
        assertThat(firstProfile).isNotNull();
        assertThat(firstProfile.getLanguage()).isEqualTo(Language.KO);

        // when - 동일 유저 다른 조건으로 재등록 (EN, US)
        MatchResultDTO result = enqueueUser(0, Language.EN, Region.US);

        // then
        assertThat(result.status()).isEqualTo(MatchStatus.WAITING);

        MatchingProfile newProfile = queueManager.getProfile(getExternalId(0));
        assertThat(newProfile).isNotNull();
        assertThat(newProfile.getLanguage()).isEqualTo(Language.EN);
        assertThat(newProfile.getRegion()).isEqualTo(Region.US);
    }

    @DisplayName("requestMatch: 활성 세션 중 재요청 → 세션 종료 후 대기열 등록")
    @Test
    @Order(4)
    void requestMatch_userInActiveSession_terminatesSessionFirst() {
        // given - 세션 생성 (user 0 ↔ user 2, 둘 다 MALE)
        createSessionBetween(0, 2);
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNotNull(); // 세션 존재
        assertThat(queueManager.getProfile(getExternalId(0))).isNull(); // 대기열 X

        // when - 세션 중인 유저가 다시 매칭 요청
        MatchResultDTO result = enqueueUser(0, Language.EN, Region.US);

        // then
        assertThat(result.status()).isEqualTo(MatchStatus.WAITING);
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNull();

        MatchingProfile profile = queueManager.getProfile(getExternalId(0));
        assertThat(profile).isNotNull();
    }

    @DisplayName("disconnectUser: 대기열에 있는 유저 → 대기열에서 제거")
    @Test
    @Order(5)
    void disconnectUser_userInQueue_removesFromQueue() {
        // given
        enqueueUser(0, Language.KO, Region.KR);
        assertThat(queueManager.getProfile(getExternalId(0))).isNotNull();

        // when
        matchingService.disconnectUser(getExternalId(0), System.currentTimeMillis());

        // then
        assertThat(queueManager.getProfile(getExternalId(0))).isNull();

        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("disconnectUser: 활성 세션 중인 유저 → 세션 종료")
    @Test
    @Order(6)
    void disconnectUser_userInSession_closesSession() {
        // given - 세션 생성 (user 0 ↔ user 2)
        createSessionBetween(0, 2);
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNotNull();
        assertThat(sessionManager.getSessionByUserId(getExternalId(2))).isNotNull();

        // when
        matchingService.disconnectUser(getExternalId(0), System.currentTimeMillis());

        // then - 양쪽 모두 세션 삭제
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNull();
        assertThat(sessionManager.getSessionByUserId(getExternalId(2))).isNull();
    }

    @DisplayName("disconnectUser: 어디에도 없는 유저 → 예외 없음")
    @Test
    @Order(7)
    void disconnectUser_userNotInQueueOrSession_noException() {
        // when & then
        assertThatCode(() -> matchingService.disconnectUser(getExternalId(0), System.currentTimeMillis()))
                .doesNotThrowAnyException();
    }

    @DisplayName("processMatchingQueue: 빈 대기열 → 아무 동작 없음")
    @Test
    @Order(8)
    void processMatchingQueue_emptyQueue_doesNothing() {
        // when
        matchingService.processMatchingQueue();

        // then
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        assertThat(sizes).isEmpty();
    }

    @DisplayName("processMatchingQueue: 동일 조건 2명 → 1라운드 매칭 성공")
    @Test
    @Order(9)
    void processMatchingQueue_twoUsersExactMatch_matchesInRound1() {
        // given - 동일 조건 MALE 2명 (index 0, 2)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then - 양쪽 세션 존재
        SessionData session0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData session2 = sessionManager.getSessionByUserId(getExternalId(2));
        assertThat(session0).isNotNull();
        assertThat(session2).isNotNull();
        assertThat(session0.sessionId()).isEqualTo(session2.sessionId());

        // 프로필 삭제됨
        assertThat(queueManager.getProfile(getExternalId(0))).isNull();
        assertThat(queueManager.getProfile(getExternalId(2))).isNull();

        // 큐 비어있음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("processMatchingQueue: 동일 조건 4명 → 2쌍 매칭")
    @Test
    @Order(10)
    void processMatchingQueue_fourUsersExactMatch_matchesTwoPairs() {
        // given - 동일 조건 MALE 4명 (index 0, 2, 4, 6)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);
        enqueueUser(4, Language.KO, Region.KR);
        enqueueUser(6, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then - 4명 모두 세션 존재
        SessionData s0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData s2 = sessionManager.getSessionByUserId(getExternalId(2));
        SessionData s4 = sessionManager.getSessionByUserId(getExternalId(4));
        SessionData s6 = sessionManager.getSessionByUserId(getExternalId(6));

        assertThat(s0).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(s4).isNotNull();
        assertThat(s6).isNotNull();

        // 2개의 서로 다른 세션
        Set<UUID> sessionIds = new HashSet<>();
        sessionIds.add(s0.sessionId());
        sessionIds.add(s2.sessionId());
        sessionIds.add(s4.sessionId());
        sessionIds.add(s6.sessionId());
        assertThat(sessionIds).hasSize(2);

        // 프로필 전부 삭제
        assertThat(queueManager.getProfile(getExternalId(0))).isNull();
        assertThat(queueManager.getProfile(getExternalId(2))).isNull();
        assertThat(queueManager.getProfile(getExternalId(4))).isNull();
        assertThat(queueManager.getProfile(getExternalId(6))).isNull();
    }

    @DisplayName("processMatchingQueue: 동일 조건 홀수명 → 1명 재등록")
    @Test
    @Order(11)
    void processMatchingQueue_oddNumberSameGroup_oneLeftoverRequeued() {
        // given - 동일 조건 MALE 3명 (index 0, 2, 4)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);
        enqueueUser(4, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then - 2명 세션 존재, 1명은 프로필이 큐에 남음
        int matchedCount = 0;
        int remainingCount = 0;
        for (int idx : new int[]{0, 2, 4}) {
            SessionData session = sessionManager.getSessionByUserId(getExternalId(idx));
            if (session != null) {
                matchedCount++;
            } else {
                remainingCount++;
                // 재등록된 유저는 큐에 남아있음 (requeueAll은 프로필 Bucket을 저장하지 않으나 기존 프로필 TTL 내)
            }
        }
        assertThat(matchedCount).isEqualTo(2);
        assertThat(remainingCount).isEqualTo(1);

        // 큐에 1명 남음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalRemaining = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalRemaining).isEqualTo(1);
    }

    @DisplayName("processMatchingQueue: 동일 성별+언어, 다른 지역 → 2라운드 매칭")
    @Test
    @Order(12)
    void processMatchingQueue_sameGenderLanguageDiffRegion_matchesInRound2() {
        // given - MALE, KO, 다른 지역 (KR vs US)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.US);

        // when
        matchingService.processMatchingQueue();

        // then - 2라운드에서 매칭
        SessionData s0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData s2 = sessionManager.getSessionByUserId(getExternalId(2));
        assertThat(s0).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(s0.sessionId()).isEqualTo(s2.sessionId());

        // 큐 비어있음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("processMatchingQueue: 1라운드 + 2라운드 매칭 혼합")
    @Test
    @Order(13)
    void processMatchingQueue_mixedRound1AndRound2() {
        // given
        // R1 매칭 쌍: user0(M,KO,KR) + user2(M,KO,KR)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);
        // R2에서 매칭될 후보: user4(M,KO,US) — 홀수라 R1 남음 → R2에서 짝 없으면 R3
        enqueueUser(4, Language.KO, Region.US);

        // when
        matchingService.processMatchingQueue();

        // then - R1 매칭된 2명 세션 존재
        SessionData s0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData s2 = sessionManager.getSessionByUserId(getExternalId(2));
        assertThat(s0).isNotNull();
        assertThat(s2).isNotNull();

        // user4는 홀수로 R1 남고, R2에서도 혼자 → R3에서도 혼자 → 재등록
        SessionData s4 = sessionManager.getSessionByUserId(getExternalId(4));
        assertThat(s4).isNull();

        // 큐에 1명 남음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalRemaining = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalRemaining).isEqualTo(1);
    }

    @DisplayName("processMatchingQueue: 다른 언어+지역, 6명 이하 → 3라운드 MergedGroup 매칭")
    @Test
    @Order(14)
    void processMatchingQueue_differentLanguageAndRegion_matchesInRound3MergedGroup() {
        // given - 각각 다른 (Language, Region) 쌍, 같은 성별 MALE → 각 그룹 1명씩
        // R1: 각 그룹 1명 → 전부 남음
        // R2: 리그룹(성별:언어) → 각각 1명 → 전부 남음
        // R3: remaining ≤ 6 → MergedGroup → 2쌍 매칭
        enqueueUser(0, Language.KO, Region.KR);  // MALE
        enqueueUser(2, Language.EN, Region.US);  // MALE
        enqueueUser(4, Language.FR, Region.FR);  // MALE
        enqueueUser(6, Language.ES, Region.ES);  // MALE

        // when
        matchingService.processMatchingQueue();

        // then - 4명 모두 매칭 (MergedGroup)
        SessionData s0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData s2 = sessionManager.getSessionByUserId(getExternalId(2));
        SessionData s4 = sessionManager.getSessionByUserId(getExternalId(4));
        SessionData s6 = sessionManager.getSessionByUserId(getExternalId(6));

        assertThat(s0).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(s4).isNotNull();
        assertThat(s6).isNotNull();

        // 큐 비어있음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("processMatchingQueue: 6명 초과 → 3라운드 마지막 세그먼트 제거 후 재그룹")
    @Test
    @Order(15)
    void processMatchingQueue_moreThan6Remaining_round3GroupsByLastSegment() {
        // given - 8명 MALE, 각각 다른 (Language, Region) → R1 각 1명씩 → R2 각 1명씩 → R3에서 > 6
        // 8개의 서로 다른 Language:Region 조합 사용
        Language[] langs = {Language.KO, Language.EN, Language.FR, Language.ES, Language.DE, Language.PT, Language.AR, Language.HI};
        Region[] regions = {Region.KR, Region.US, Region.FR, Region.ES, Region.DE, Region.BR, Region.MA, Region.IN};

        for (int i = 0; i < 8; i++) {
            enqueueUser(i * 2, langs[i], regions[i]); // even index = MALE
        }

        // when
        matchingService.processMatchingQueue();

        // then - 대부분 매칭, 홀수면 1명 재등록
        int matchedCount = 0;
        int unmatchedCount = 0;
        for (int i = 0; i < 8; i++) {
            SessionData session = sessionManager.getSessionByUserId(getExternalId(i * 2));
            if (session != null) {
                matchedCount++;
            } else {
                unmatchedCount++;
            }
        }

        // 8명 → 4쌍 또는 3쌍+재등록2명 (그룹핑 방식에 따라 다름)
        assertThat(matchedCount + unmatchedCount).isEqualTo(8);
        assertThat(matchedCount).isGreaterThanOrEqualTo(6); // 최소 3쌍
    }

    @DisplayName("processMatchingQueue: 1명만 대기 → 3라운드 후 재등록")
    @Test
    @Order(16)
    void processMatchingQueue_singleUser_requeuedAfterAllRounds() {
        // given - 1명만 등록
        enqueueUser(0, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then - 세션 없음
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNull();

        // 큐에 1명 남음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalRemaining = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalRemaining).isEqualTo(1);
    }

    @DisplayName("processMatchingQueue: 세션 데이터 검증 - 참여자 정보")
    @Test
    @Order(17)
    void processMatchingQueue_verifySessionData_correctParticipants() {
        // given - MALE 2명 매칭
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then
        SessionData session = sessionManager.getSessionByUserId(getExternalId(0));
        assertThat(session).isNotNull();
        assertThat(session.participants()).hasSize(2);

        // 참여자 정보 확인
        Set<String> participantIds = new HashSet<>();
        for (ParticipantData p : session.participants()) {
            participantIds.add(p.userExternalId());
            assertThat(p.region()).isEqualTo(Region.KR);
            assertThat(p.gender()).isEqualTo(Gender.MALE);
            assertThat(p.language()).isEqualTo(Language.KO);
        }
        assertThat(participantIds).containsExactlyInAnyOrder(getExternalId(0), getExternalId(2));
    }

    @DisplayName("processMatchingQueue: 매칭 후 프로필 삭제 검증")
    @Test
    @Order(18)
    void processMatchingQueue_verifyProfileCleanup_profilesDeletedAfterMatch() {
        // given - 4명 매칭
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);
        enqueueUser(4, Language.KO, Region.KR);
        enqueueUser(6, Language.KO, Region.KR);

        // when
        matchingService.processMatchingQueue();

        // then - 전원 프로필 삭제
        assertThat(queueManager.getProfile(getExternalId(0))).isNull();
        assertThat(queueManager.getProfile(getExternalId(2))).isNull();
        assertThat(queueManager.getProfile(getExternalId(4))).isNull();
        assertThat(queueManager.getProfile(getExternalId(6))).isNull();

        // 큐 비어있음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("processMatchingQueue: 다양한 성별/언어/지역 혼합 → 올바른 그룹핑")
    @Test
    @Order(19)
    void processMatchingQueue_mixedGendersLanguagesRegions_correctGrouping() {
        // given - 10명 다양한 조건
        // R1 쌍: (0,2) MALE,KO,KR  /  (1,3) FEMALE,KO,KR
        enqueueUser(0, Language.KO, Region.KR);  // MALE
        enqueueUser(2, Language.KO, Region.KR);  // MALE
        enqueueUser(1, Language.KO, Region.KR);  // FEMALE
        enqueueUser(3, Language.KO, Region.KR);  // FEMALE
        // 나머지 6명: 각각 다른 조건 → R1 남음 → R2/R3에서 매칭
        enqueueUser(4, Language.EN, Region.US);   // MALE
        enqueueUser(6, Language.FR, Region.FR);   // MALE
        enqueueUser(8, Language.ES, Region.ES);   // MALE
        enqueueUser(5, Language.EN, Region.US);   // FEMALE
        enqueueUser(7, Language.FR, Region.FR);   // FEMALE
        enqueueUser(9, Language.ES, Region.ES);   // FEMALE

        // when
        matchingService.processMatchingQueue();

        // then - 전원 세션 존재
        for (int i = 0; i < 10; i++) {
            SessionData session = sessionManager.getSessionByUserId(getExternalId(i));
            assertThat(session).as("User %d should have session", i).isNotNull();
        }
    }

    @DisplayName("processMatchingQueue: 성별 다른 2명 → R3 MergedGroup 매칭")
    @Test
    @Order(20)
    void processMatchingQueue_genderCrossoverInRound3() {
        // given - MALE 1명 + FEMALE 1명, 다른 언어/지역
        // R1: 각각 다른 큐 → 각 1명 → 남음
        // R2: 리그룹(성별:언어) → 각 1명 → 남음
        // R3: remaining ≤ 6 → MergedGroup → 매칭
        enqueueUser(0, Language.KO, Region.KR);  // MALE
        enqueueUser(1, Language.EN, Region.US);  // FEMALE

        // when
        matchingService.processMatchingQueue();

        // then - R3에서 merge 후 매칭
        SessionData s0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData s1 = sessionManager.getSessionByUserId(getExternalId(1));
        assertThat(s0).isNotNull();
        assertThat(s1).isNotNull();
        assertThat(s0.sessionId()).isEqualTo(s1.sessionId());
    }

    @DisplayName("[대량] 동일 조건 매칭")
    @Test
    @Order(21)
    void processMatchingQueue_SameGroupUsers_PairsMatched() {
        // given - even index = MALE
        int userCount = 50; // 변경하면서 테스트

        for (int i = 0; i < userCount; i++) {
            enqueueUser(i * 2, Language.KO, Region.KR); // 0,2,4,...,98 → all MALE
        }

        // when
        matchingService.processMatchingQueue();

        // then - 전원 세션 존재
        for (int i = 0; i < userCount; i++) {
            SessionData session = sessionManager.getSessionByUserId(getExternalId(i * 2));
            assertThat(session).as("User index %d should have session", i * 2).isNotNull();
        }

        // 프로필 전부 삭제
        for (int i = 0; i < userCount; i++) {
            assertThat(queueManager.getProfile(getExternalId(i * 2))).isNull();
        }

        // 큐 비어있음
        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int totalSize = sizes.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalSize).isZero();
    }

    @DisplayName("[대량] 다양한 조건 → 전원 매칭 또는 재등록")
    @Test
    @Order(22)
    void processMatchingQueue_sers_diverseGroups_allAccountedFor() {
        // given - 다양한 조건
        int userCount = 50; // 변경하면서 테스트

        Language[] languages = Language.values();
        Region[] regions = Region.values();

        for (int i = 0; i < userCount; i++) {
            Language lang = languages[i % (languages.length - 1)]; // NONE 제외
            Region region = regions[i % (regions.length - 1)];     // NONE 제외
            enqueueUser(i, lang, region);
        }

        // when
        matchingService.processMatchingQueue();

        // then - 전원: 매칭됨 또는 큐에 재등록
        int matchedCount = 0;
        for (int i = 0; i < userCount; i++) {
            SessionData session = sessionManager.getSessionByUserId(getExternalId(i));
            if (session != null) {
                matchedCount++;
            }
        }

        Map<String, Integer> sizes = queueManager.getActiveQueueSizes();
        int queueRemaining = sizes.values().stream().mapToInt(Integer::intValue).sum();

        assertThat(matchedCount + queueRemaining).isEqualTo(userCount);
    }

    @DisplayName("[성능] 매칭 처리 시간 검증")
    @Test
    @Order(23)
    void processMatchingQueue_100Users_performance() {
        Language[] languages = Language.values();
        Region[] regions = Region.values();

        // given
        int userCount = 1000; // 변경하면서 테스트
        ExecutorService executor = Executors.newFixedThreadPool(userCount);

        List<CompletableFuture<Void>> futures = IntStream.range(0, userCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Language lang = languages[i % (languages.length - 1)];
                    Region region = regions[i % (regions.length - 1)];
                    enqueueUser(i, lang, region);
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // when
        LocalDateTime start = LocalDateTime.now();
        matchingService.processMatchingQueue();
        LocalDateTime end   = LocalDateTime.now();

        // then - 3초 이내 완료
        long elapsed = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("소요 시간: " + elapsed + "ms");
        System.out.println("[성능] processMatchingQueue 다른 큐 (" + userCount + "명): " + elapsed + "ms");

        assertThat(elapsed).as("%d명 매칭은 %dms 이내 완료되어야 함", userCount, 500).isLessThan(500);
    }

    @DisplayName("processMatchingQueue: 프로필 만료 → 매칭 없음")
    @Test
    @Order(24)
    void processMatchingQueue_profilesExpired_noMatches() {
        // given - 2명 등록 후 프로필 수동 삭제 (만료 시뮬레이션)
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);

        // 프로필 Bucket 수동 삭제
        redissonClient.getBucket(RedisKeyBuilder.buildProfileKey(getExternalId(0))).delete();
        redissonClient.getBucket(RedisKeyBuilder.buildProfileKey(getExternalId(2))).delete();

        // when
        matchingService.processMatchingQueue();

        // then - 프로필이 없으므로 매칭 불가
        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNull();
        assertThat(sessionManager.getSessionByUserId(getExternalId(2))).isNull();
    }

    @DisplayName("processMatchingQueue: 연속 2회 호출 → 두 번째는 빈 대기열")
    @Test
    @Order(25)
    void processMatchingQueue_calledTwice_secondCallIsNoop() {
        // given - 2명 등록
        enqueueUser(0, Language.KO, Region.KR);
        enqueueUser(2, Language.KO, Region.KR);

        // when - 1차 호출
        matchingService.processMatchingQueue();

        // 1차 매칭 확인
        SessionData session1 = sessionManager.getSessionByUserId(getExternalId(0));
        assertThat(session1).isNotNull();

        // when - 2차 호출
        matchingService.processMatchingQueue();

        // then - 추가 세션 생성 없음 (기존 세션 유지)
        SessionData sessionAfter = sessionManager.getSessionByUserId(getExternalId(0));
        assertThat(sessionAfter).isNotNull();
        assertThat(sessionAfter.sessionId()).isEqualTo(session1.sessionId());
    }

    @DisplayName("통합: 매칭 → 연결 → 연결 해제 전체 생명주기")
    @Test
    @Order(26)
    void fullLifecycle_matchThenDisconnect() {
        // Step 1: requestMatch → WAITING
        MatchResultDTO result0 = enqueueUser(0, Language.KO, Region.KR);
        MatchResultDTO result2 = enqueueUser(2, Language.KO, Region.KR);
        assertThat(result0.status()).isEqualTo(MatchStatus.WAITING);
        assertThat(result2.status()).isEqualTo(MatchStatus.WAITING);

        // Step 2: processMatchingQueue → 세션 생성
        matchingService.processMatchingQueue();

        SessionData session0 = sessionManager.getSessionByUserId(getExternalId(0));
        SessionData session2 = sessionManager.getSessionByUserId(getExternalId(2));
        assertThat(session0).isNotNull();
        assertThat(session2).isNotNull();
        assertThat(session0.sessionId()).isEqualTo(session2.sessionId());

        // 프로필 정리 확인
        assertThat(queueManager.getProfile(getExternalId(0))).isNull();
        assertThat(queueManager.getProfile(getExternalId(2))).isNull();

        // Step 3: disconnectUser → 세션 삭제
        matchingService.disconnectUser(getExternalId(0), System.currentTimeMillis());

        assertThat(sessionManager.getSessionByUserId(getExternalId(0))).isNull();
        assertThat(sessionManager.getSessionByUserId(getExternalId(2))).isNull();
    }

    // === 헬퍼 메서드 ===

    private List<UserInfoDTO> createUserList(int count) {
        return LongStream.range(0, count)
                .mapToObj(i -> UserInfoDTO.builder()
                        .id(i)
                        .externalId(UUID.randomUUID().toString())
                        .username("testUser" + i)
                        .email("test" + i + "@example.com")
                        .provider(OAuthProvider.GOOGLE)
                        .providerId("provider-id-" + i)
                        .role(Role.ROLE_USER)
                        .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                        .birthDate(LocalDate.now())
                        .build()
                )
                .toList();
    }

    private MatchResultDTO enqueueUser(int index, Language language, Region region) {
        return matchingService.requestMatch(
                getExternalId(index),
                region,
                createMatchRequest(language, region, null)
        );
    }

    private MatchRequestDTO createMatchRequest(Language language, Region region, Gender gender) {
        return new MatchRequestDTO(region, gender, language);
    }

    private String getExternalId(int index) {
        return testUsers.get(index).getExternalId();
    }

    private void createSessionBetween(int idx1, int idx2) {
        UserInfoDTO user1 = testUsers.get(idx1);
        UserInfoDTO user2 = testUsers.get(idx2);
        SessionData session = new SessionData(
                LocalDateTime.now(),
                List.of(
                        new ParticipantData(user1.getId(), user1.getExternalId(), Region.KR, user1.getGender(), Language.KO),
                        new ParticipantData(user2.getId(), user2.getExternalId(), Region.KR, user2.getGender(), Language.KO)
                )
        );
        sessionManager.saveSessions(List.of(session));
    }

}
