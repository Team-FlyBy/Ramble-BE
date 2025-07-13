package com.flyby.ramble.report.controller;

import com.flyby.ramble.report.dto.ReportUserRequestDTO;
import com.flyby.ramble.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/reports")
    public ResponseEntity<Void> reportByUser(@RequestPart("request") ReportUserRequestDTO requestDTO,
                                             @RequestPart(value = "peerVideoSnapshot", required = false) MultipartFile peerVideoSnapshot) {
        log.debug("User report request received: {}", requestDTO);
        reportService.reportByUser(requestDTO, peerVideoSnapshot);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
