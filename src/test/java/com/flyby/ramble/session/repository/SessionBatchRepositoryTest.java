package com.flyby.ramble.session.repository;

import com.flyby.ramble.common.config.QuerydslConfig;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.model.Session;
import com.flyby.ramble.session.model.SessionParticipant;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("SessionBatchRepository Batch Insert 테스트")
@DataJpaTest
@Import({QuerydslConfig.class, SessionBatchRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionBatchRepositoryTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?rewriteBatchedStatements=true");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // 로깅은 상황에 맞게 변경해가면서 테스트
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
    }

    @Autowired
    private SessionBatchRepository sessionBatchRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
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
            testUsers.add(userRepository.save(user));
        }
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("saveSessionsWithParticipants: 단일 세션 저장")
    @Test
    @Order(1)
    void saveSessionsWithParticipants_singleSession() {
        // given
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);

        SessionData sessionData = new SessionData(
                LocalDateTime.now(),
                List.of(
                        new ParticipantData(user1.getId(), user1.getExternalId().toString(), Region.KR, Gender.MALE, Language.KO),
                        new ParticipantData(user2.getId(), user2.getExternalId().toString(), Region.KR, Gender.FEMALE, Language.KO)
                )
        );

        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(List.of(sessionData));
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(1);

        Session savedSession = sessions.get(0);
        assertThat(savedSession.getExternalId()).isEqualTo(sessionData.sessionId());
        assertThat(savedSession.getStartedAt()).isNotNull();

        // participants 검증
        List<SessionParticipant> participants = savedSession.getParticipants();
        assertThat(participants).hasSize(2);
        assertThat(participants)
                .extracting(p -> p.getUser().getId())
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("saveSessionsWithParticipants: 빈 리스트 → 저장 없음")
    @Test
    @Order(2)
    void saveSessionsWithParticipants_emptyList_noSave() {
        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(Collections.emptyList());
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).isEmpty();

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("saveSessionsWithParticipants: null → 저장 없음")
    @Test
    @Order(3)
    void saveSessionsWithParticipants_nullList_noSave() {
        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(null);
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).isEmpty();

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("saveSessionsWithParticipants: 참가자 데이터 저장")
    @Test
    @Order(4)
    void saveSessionsWithParticipants_participantData_savedCorrectly() {
        // given
        User user1 = testUsers.get(0);
        User user2 = testUsers.get(1);
        Region expectedRegion = Region.US;
        Gender expectedGender = Gender.MALE;
        Language expectedLanguage = Language.EN;

        SessionData sessionData = new SessionData(
                LocalDateTime.now(),
                List.of(
                        new ParticipantData(user1.getId(), user1.getExternalId().toString(), expectedRegion, expectedGender, expectedLanguage),
                        new ParticipantData(user2.getId(), user2.getExternalId().toString(), expectedRegion, expectedGender, expectedLanguage)
                )
        );

        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(List.of(sessionData));
        LocalDateTime end   = LocalDateTime.now();

        // then
        Session session = sessionRepository.findByExternalId(sessionData.sessionId()).orElseThrow();
        List<SessionParticipant> participants = session.getParticipants();

        // participants 정보 검증
        assertThat(participants).hasSize(2);
        assertThat(participants)
                .extracting(
                        p -> p.getUser().getId(),
                        SessionParticipant::getRegion,
                        SessionParticipant::getGender,
                        SessionParticipant::getLanguage
                )
                .containsExactly(
                        tuple(user1.getId(), expectedRegion, expectedGender, expectedLanguage),
                        tuple(user2.getId(), expectedRegion, expectedGender, expectedLanguage)
                );

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("saveSessionsWithParticipants: 다수 세션 저장")
    @Test
    @Order(5)
    void saveSessionsWithParticipants_multipleSessions() {
        // given
        int sessionCount = 5;
        List<SessionData> sessionDataList = createSessionDataList(sessionCount);

        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(sessionDataList);
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(sessionCount);

        // 각 세션에 참가자가 있는지 확인
        for (Session session : sessions) {
            assertThat(session.getParticipants()).isNotEmpty();
        }

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("saveSessionsWithParticipants: 대량 세션 (BATCH_SIZE 초과)")
    @Test
    @Order(6)
    void saveSessionsWithParticipants_largeBatch_exceedsBatchSize() {
        // given - BATCH_SIZE(500)를 초과하는 세션 생성
        int sessionCount = 600;
        List<SessionData> sessionDataList = createSessionDataList(sessionCount);

        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(sessionDataList);
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(sessionCount);

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
    }

    @DisplayName("[성능] 대량 세션 배치 저장")
    @Test
    @Order(7)
    void saveSessionsWithParticipants_performance() {
        // given
        int sessionCount = 2000; // 변경하면서 테스트
        List<SessionData> sessionDataList = createSessionDataList(sessionCount);

        // when
        LocalDateTime start = LocalDateTime.now();
        sessionBatchRepository.saveSessionsWithParticipants(sessionDataList);
        LocalDateTime end   = LocalDateTime.now();

        // then
        List<Session> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(sessionCount);

        long elapsedTime = ChronoUnit.MILLIS.between(start, end);
        // 합리적인 시간 내에 완료되어야 함 (10초 이내)
        assertThat(elapsedTime).isLessThan(10000);

        System.out.println("삽입 시작: " + start);
        System.out.println("삽입 완료: " + end);
        System.out.println("1000개 세션 배치 저장 소요 시간: " + elapsedTime + "ms");
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
