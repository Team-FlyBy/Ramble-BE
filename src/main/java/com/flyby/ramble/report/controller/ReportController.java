package com.flyby.ramble.report.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.report.dto.ReportUserRequestDTO;
import com.flyby.ramble.report.service.ReportService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
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
public class ReportController {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ReportService reportService;

    @PostMapping(value = "/user-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> reportByUser(@RequestPart("request") String requestJson,
                                             @RequestPart(value = "peerVideoSnapshot", required = false) MultipartFile peerVideoSnapshot) {
        /**
         * TODO 현재는 requestBody에서 신고대상자 ID를 가져오고 있으나 토큰이나 인증 정보에서 가져오도록 수정 필요
         */

        ReportUserRequestDTO requestDTO;
        try {
            requestDTO = objectMapper.readValue(requestJson, ReportUserRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Set<ConstraintViolation<ReportUserRequestDTO>> violations = validator.validate(requestDTO);
        if (!validator.validate(requestDTO).isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        log.debug("User report request received: {}", requestDTO);

        reportService.reportByUser(requestDTO, peerVideoSnapshot);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
