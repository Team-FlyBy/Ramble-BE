package com.flyby.ramble.report.controller;

import com.flyby.ramble.report.dto.AutoNudeDetectionCommandDTO;
import com.flyby.ramble.report.dto.AutoNudeDetectionRequestDTO;
import com.flyby.ramble.report.service.NudeDetectionService;
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
public class NudeDetectionController {
    private final NudeDetectionService nudeDetectionService;

    @PostMapping("/auto-nude-detection")
    public ResponseEntity<Void> submitAutoNudeDetection(@RequestPart("request") AutoNudeDetectionRequestDTO requestDTO,
                                                 @RequestPart(value = "peerVideoSnapshot", required = true) MultipartFile peerVideoSnapshot) {
        log.debug("Auto report request received: {}", requestDTO);

        nudeDetectionService.requestAutoDetection(
                AutoNudeDetectionCommandDTO.builder()
                    .userUuid(requestDTO.getUserUuid())
                    .peerVideoSnapshot(peerVideoSnapshot)
                    .build()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
