package com.flyby.ramble.report.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.report.dto.AutoNudeDetectionCommandDTO;
import com.flyby.ramble.report.dto.AutoNudeDetectionRequestDTO;
import com.flyby.ramble.report.service.NudeDetectionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NudeDetectionController {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final NudeDetectionService nudeDetectionService;

    @PostMapping(value = "/auto-nude-detection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> submitAutoNudeDetection(@RequestPart("request") String requestJson,
                                                 @RequestPart(value = "peerVideoSnapshot") MultipartFile peerVideoSnapshot) {

        /**
         * TODO 현재는 requestBody에서 신고대상자 ID를 가져오고 있으나 토큰이나 인증 정보에서 가져오도록 수정 필요
         */

        AutoNudeDetectionRequestDTO requestDTO;
        try {
            requestDTO = objectMapper.readValue(requestJson, AutoNudeDetectionRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Set<ConstraintViolation<AutoNudeDetectionRequestDTO>> violations = validator.validate(requestDTO);
        if (!validator.validate(requestDTO).isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        nudeDetectionService.requestAutoDetection(
                AutoNudeDetectionCommandDTO.builder()
                    .userUuid(requestDTO.getUserUuid())
                    .peerVideoSnapshot(peerVideoSnapshot)
                    .build()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
