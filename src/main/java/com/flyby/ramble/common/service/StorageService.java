package com.flyby.ramble.common.service;


import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * 스토리지에 파일 생성
     * @param newFilePath 생성할 파일 경로 eg. /users/user/filename.png
     * @param file 생성할 파일
     * @return 생성된 파일 경로
     */
    String uploadFile(String newFilePath, MultipartFile file);

    /**
     * 스토리지에 파일 제거
     * @param filepath 파일 경로 eg. /users/user/filename.png
     */
    void removeFile(String filepath);
}
