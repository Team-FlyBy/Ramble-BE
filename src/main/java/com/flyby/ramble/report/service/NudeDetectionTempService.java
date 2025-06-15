package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.DetectNudeCommandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NudeDetectionTempService implements NudeDetectionService {

    public void detect(DetectNudeCommandDTO requestDTO) {
        // TEMP
    }
}
