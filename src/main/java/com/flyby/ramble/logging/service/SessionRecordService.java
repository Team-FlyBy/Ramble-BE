package com.flyby.ramble.logging.service;

import com.flyby.ramble.logging.dto.CreateSessionRecordCommandDTO;
import com.flyby.ramble.logging.model.meta.SessionRecord;
import com.flyby.ramble.logging.repository.meta.SessionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionRecordService {
    private final SessionRecordRepository sessionRecordRepository;

    /**
     * 세션 레코드 생성
     * @param commandDTO
     */
    @Transactional
    public void createSessionRecord(CreateSessionRecordCommandDTO commandDTO) {
        SessionRecord sessionRecord = SessionRecord.builder()
                .uuid(commandDTO.getSessionUuid())
                .startedAt(commandDTO.getStartedAt())
                .endedAt(commandDTO.getEndedAt())
                .build();

        sessionRecordRepository.save(sessionRecord);
    }
}
