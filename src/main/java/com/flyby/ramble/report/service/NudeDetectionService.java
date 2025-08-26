package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.AutoNudeDetectionCommandDTO;
import com.flyby.ramble.report.dto.DetectNudeCommandDTO;

public interface NudeDetectionService {
    void requestDetection(DetectNudeCommandDTO requestDTO);
    void requestAutoDetection(AutoNudeDetectionCommandDTO commandDTO);
}
