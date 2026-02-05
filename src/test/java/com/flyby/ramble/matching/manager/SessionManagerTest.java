package com.flyby.ramble.matching.manager;

import com.flyby.ramble.common.config.RedissonConfig;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.matching.constants.MatchingConstants;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.util.RedisKeyBuilder;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.event.SessionEndedEvent;
import com.flyby.ramble.session.service.SessionService;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DisplayName("SessionManager 테스트 (실제 Redis)")
@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RedissonConfig.class,
        SessionManager.class,
        SessionManagerTest.TestConfig.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionManagerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ApplicationEventPublisher mockEventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }
    }

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private SessionService sessionService;

    @Autowired
    private SessionManager sessionManager;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        Mockito.reset(eventPublisher);

        // 테스트용 사용자 생성
        testUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .username("testUser" + i)
                    .email("test" + i + "@example.com")
                    .provider(OAuthProvider.GOOGLE)
                    .providerId("provider-id-" + i)
                    .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                    .build();
            testUsers.add(user);
        }
    }

    @AfterEach
    void tearDown() {
        redissonClient.getKeys().flushdb();
    }

    @DisplayName("saveSessions: 저장 및 조회")
    @Test
    @Order(1)
    void saveSessions_savesAndRetrieves() {
        // given
        List<SessionData> sessionList = createSessionDataList(3);
        SessionData session= sessionList.get(0);
        List<ParticipantData> participants = session.participants();

        // when
        sessionManager.saveSessions(sessionList);

        // then
        SessionData saveSession = sessionManager.getSession(session.sessionId().toString());

        assertThat(saveSession).isNotNull();
        assertThat(saveSession.sessionId()).isEqualTo(session.sessionId());
        assertThat(saveSession.startedAt()).isEqualTo(session.startedAt());

        // participants 검증
        List<ParticipantData> saveSessionParticipants = saveSession.participants();
        assertThat(saveSessionParticipants).containsExactlyElementsOf(participants);
    }

    @DisplayName("saveSessions: 빈 리스트 → 예외 없음")
    @Test
    @Order(2)
    void saveSessions_emptyList_noException() {
        // when & then
        assertThatCode(() -> sessionManager.saveSessions(List.of()))
                .doesNotThrowAnyException();
    }

    @DisplayName("saveSessions: 배치 크기 초과 → 청크 단위 처리")
    @Test
    @Order(3)
    void saveSessions_largeList_processesInChunks() {
        // given: REDIS_BATCH_SIZE(1000)보다 큰 세션 리스트 생성
        int totalSessions = MatchingConstants.REDIS_BATCH_SIZE + 500;
        List<SessionData> largeSessions = createSessionDataList(totalSessions);

        // when
        sessionManager.saveSessions(largeSessions);

        // then: 모든 세션이 정상적으로 저장되었는지 확인
        int savedCount = 0;
        for (SessionData session : largeSessions) {
            SessionData retrieved = sessionManager.getSession(session.sessionId().toString());
            if (retrieved != null) {
                savedCount++;
            }
        }
        assertThat(savedCount).isEqualTo(totalSessions);
    }

    @DisplayName("saveSessions: sessionId와 userId 참조 키 생성")
    @Test
    @Order(4)
    void saveSessions_createsSessionAndUserKeys() {
        // when
        List<SessionData> sessionList = createSessionDataList(1);
        SessionData session = sessionList.get(0);
        sessionManager.saveSessions(sessionList);

        // then: sessionId 키 확인
        String sessionKey = RedisKeyBuilder.buildSessionKey(session.sessionId().toString());
        RBucket<SessionData> sessionBucket = redissonClient.getBucket(sessionKey);
        assertThat(sessionBucket.isExists()).isTrue();
        assertThat(sessionBucket.get().sessionId()).isEqualTo(session.sessionId());

        // then: userId 참조 키 확인
        for (ParticipantData participant : session.participants()) {
            String userKey = RedisKeyBuilder.buildSessionUserKey(participant.userExternalId());
            RBucket<String> userBucket = redissonClient.getBucket(userKey);
            assertThat(userBucket.isExists()).isTrue();
            assertThat(userBucket.get()).isEqualTo(session.sessionId().toString());
        }
    }

    @DisplayName("closeSession: 관련 키 모두 삭제")
    @Test
    @Order(5)
    void closeSession_deletesAllKeys() {
        // given
        List<SessionData> sessionList = createSessionDataList(1);
        SessionData session = sessionList.get(0);
        sessionManager.saveSessions(sessionList);

        // 저장 확인
        String sessionKey = RedisKeyBuilder.buildSessionKey(session.sessionId().toString());
        assertThat(redissonClient.getBucket(sessionKey).isExists()).isTrue();

        // when
        sessionManager.closeSession(session);

        // then: sessionId 키 삭제 확인
        assertThat(redissonClient.getBucket(sessionKey).isExists()).isFalse();

        // then: userId 참조 키 삭제 확인
        for (ParticipantData participant : session.participants()) {
            String userKey = RedisKeyBuilder.buildSessionUserKey(participant.userExternalId());
            assertThat(redissonClient.getBucket(userKey).isExists()).isFalse();
        }
    }

    @DisplayName("closeSession: null → 예외 없음")
    @Test
    @Order(6)
    void closeSession_null_noException() {
        // when & then
        assertThatCode(() -> sessionManager.closeSession(null))
                .doesNotThrowAnyException();
    }

    @DisplayName("closeSession: SessionEndedEvent 발행")
    @Test
    @Order(7)
    void closeSession_publishesEvent() {
        // given
        List<SessionData> sessionList = createSessionDataList(1);
        SessionData session = sessionList.get(0);
        sessionManager.saveSessions(sessionList);

        // when
        sessionManager.closeSession(session);

        // then: CompletableFuture.runAsync()로 비동기 실행되므로 timeout을 사용하여 검증
        ArgumentCaptor<SessionEndedEvent> eventCaptor = ArgumentCaptor.forClass(SessionEndedEvent.class);
        verify(eventPublisher, timeout(1000).times(1)).publishEvent(eventCaptor.capture());

        SessionEndedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSessionUuid()).isEqualTo(session.sessionId());
        assertThat(capturedEvent.getStartedAt()).isEqualTo(session.startedAt());
        assertThat(capturedEvent.getEndedAt()).isAfterOrEqualTo(session.startedAt());
    }

    @DisplayName("getSession: 존재하지 않는 세션 → null")
    @Test
    @Order(8)
    void getSession_nonExistent_returnsNull() {
        // when
        SessionData session = sessionManager.getSession("non-existent-session-id");

        // then
        assertThat(session).isNull();
    }

    @DisplayName("getSessionByUserId: 세션 조회 성공")
    @Test
    @Order(9)
    void getSessionByUserId_returnsSession() {
        // given
        List<SessionData> sessionList = createSessionDataList(1);
        SessionData session = sessionList.get(0);
        sessionManager.saveSessions(sessionList);
        String userId = session.participants().get(0).userExternalId();

        // userId -> sessionId 매핑이 저장되었는지 먼저 확인
        String userKey = RedisKeyBuilder.buildSessionUserKey(userId);
        RBucket<String> userBucket = redissonClient.getBucket(userKey);
        assertThat(userBucket.isExists()).isTrue();
        String sessionIdFromBucket = userBucket.get();
        assertThat(sessionIdFromBucket).isEqualTo(session.sessionId().toString());

        // when
        SessionData saveSession = sessionManager.getSessionByUserId(userId);

        // then
        assertThat(saveSession.sessionId()).isEqualTo(session.sessionId());
    }

    @DisplayName("getSessionByUserId: 존재하지 않는 사용자 → null")
    @Test
    @Order(10)
    void getSessionByUserId_nonExistentUser_returnsNull() {
        // when
        SessionData session = sessionManager.getSessionByUserId("non-existent-user-id");

        // then
        assertThat(session).isNull();
    }

    private List<SessionData> createSessionDataList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    User user1 = testUsers.get(i % testUsers.size());
                    User user2 = testUsers.get((i + 1) % testUsers.size());
                    return new SessionData(
                            LocalDateTime.now(),
                            List.of(
                                    new ParticipantData(user1.getId(), user1.getExternalId().toString(), Region.KR, Gender.MALE, Language.KO),
                                    new ParticipantData(user2.getId(), user2.getExternalId().toString(), Region.KR, Gender.FEMALE, Language.KO)
                            )
                    );
                })
                .toList();
    }

}
