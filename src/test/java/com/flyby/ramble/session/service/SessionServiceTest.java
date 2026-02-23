package com.flyby.ramble.session.service;

import com.flyby.ramble.common.config.AsyncConfig;
import com.flyby.ramble.matching.model.Language;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.session.dto.ParticipantData;
import com.flyby.ramble.session.dto.SessionData;
import com.flyby.ramble.session.repository.SessionBatchRepository;
import com.flyby.ramble.session.repository.SessionRepository;
import com.flyby.ramble.user.model.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("SessionService 비동기 테스트")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AsyncConfig.class,
        SessionService.class
})
class SessionServiceTest {

    @MockitoBean
    private SessionBatchRepository sessionBatchRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @Autowired
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        reset(sessionBatchRepository);
    }

    @Test
    @DisplayName("saveSessionsAsync: 별도 스레드에서 비동기 실행")
    void saveSessionsAsync_runsInSeparateThread() {
        // given
        List<SessionData> sessions = createSessionDataList(10);

        String mainThreadName = Thread.currentThread().getName();
        StringBuilder asyncThreadName = new StringBuilder();

        doAnswer(invocation -> {
            asyncThreadName.append(Thread.currentThread().getName());
            return null;
        }).when(sessionBatchRepository).saveSessionsWithParticipants(anyList());

        // when
        CompletableFuture<Void> future = sessionService.saveSessionsAsync(sessions);

        // then
        await().atMost(5, TimeUnit.SECONDS).until(future::isDone);

        assertThat(future.isCompletedExceptionally()).isFalse();
        assertThat(asyncThreadName.toString())
                .isNotEmpty()
                .isNotEqualTo(mainThreadName);
        verify(sessionBatchRepository, times(1)).saveSessionsWithParticipants(anyList());
    }

    @Test
    @DisplayName("saveSessionsAsync: 예외 발생 시 failedFuture 반환")
    void saveSessionsAsync_exceptionReturnsFailedFuture() {
        // given
        List<SessionData> sessions = createSessionDataList(1);
        doThrow(new RuntimeException("DB Error"))
                .when(sessionBatchRepository).saveSessionsWithParticipants(anyList());

        // when
        CompletableFuture<Void> future = sessionService.saveSessionsAsync(sessions);

        // then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isTrue();
            verify(sessionBatchRepository, times(1)).saveSessionsWithParticipants(anyList());
        });
    }

    @Test
    @DisplayName("saveSessionsAsync: 빈 리스트 시 저장 로직 호출 없이 완료")
    void saveSessionsAsync_emptyListSkipsSave() {
        // when
        CompletableFuture<Void> future = sessionService.saveSessionsAsync(Collections.emptyList());

        // then - @Async 프록시로 인해 비동기 완료 대기 필요
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isFalse();
            verify(sessionBatchRepository, never()).saveSessionsWithParticipants(anyList());
        });
    }

    @Test
    @DisplayName("saveSessionsAsync: null 시 저장 로직 호출 없이 완료")
    void saveSessionsAsync_nullSkipsSave() {
        // when
        CompletableFuture<Void> future = sessionService.saveSessionsAsync(null);

        // then - @Async 프록시로 인해 비동기 완료 대기 필요
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isFalse();
            verify(sessionBatchRepository, never()).saveSessionsWithParticipants(anyList());
        });
    }

    @Test
    @DisplayName("saveSessionsAsync: 여러 호출이 병렬로 실행")
    void saveSessionsAsync_multipleCallsRunInParallel() {
        // given
        List<SessionData> sessions1 = createSessionDataList(10);
        List<SessionData> sessions2 = createSessionDataList(10);

        Set<String> executionThreadNames = ConcurrentHashMap.newKeySet();

        doAnswer(invocation -> {
            executionThreadNames.add(Thread.currentThread().getName());
            return null;
        }).when(sessionBatchRepository).saveSessionsWithParticipants(anyList());

        // when
        CompletableFuture<Void> future1 = sessionService.saveSessionsAsync(sessions1);
        CompletableFuture<Void> future2 = sessionService.saveSessionsAsync(sessions2);

        // then
        await().atMost(5, TimeUnit.SECONDS).until(() -> future1.isDone() && future2.isDone());

        assertThat(executionThreadNames).hasSize(2);
        verify(sessionBatchRepository, times(2)).saveSessionsWithParticipants(anyList());
    }

    private List<SessionData> createSessionDataList(int count) {
        return LongStream.range(0, count)
                .mapToObj(i -> new SessionData(
                        LocalDateTime.now(),
                        List.of(
                                new ParticipantData(i * 2, "user-" + (i * 2) + "-uuid", Region.KR, Gender.MALE, Language.KO),
                                new ParticipantData(i * 2 + 1, "user-" + (i * 2 + 1) + "-uuid", Region.KR, Gender.FEMALE, Language.KO)
                        ))
                )
                .toList();
    }
}