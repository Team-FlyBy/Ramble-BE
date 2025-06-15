package com.flyby.ramble.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageTempService implements StorageService {

    public String uploadFile(String newFilePath, MultipartFile file) {
        // TEMP
        return null;
    }

    public void removeFile(String filepath) {
        // TEMP
    }
}
