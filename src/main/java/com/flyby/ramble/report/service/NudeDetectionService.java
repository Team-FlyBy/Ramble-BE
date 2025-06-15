package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.DetectNudeCommandDTO;

public interface NudeDetectionService {
    void detect(DetectNudeCommandDTO requestDTO);
}
