package com.flyby.ramble.common.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {
    /**
     * 스토리지에 파일 생성
     * @param newFilePath 생성할 파일 경로 eg. /users/user/filename.png
     * @param file 생성할 파일
     */
    void uploadFile(String newFilePath, MultipartFile file);


    void uploadFile(String newFilePath, byte[] fileBytes, String contentType);

    void uploadFile(String newFilePath, InputStream inputStream, long contentLength, String contentType);

    /**
     * 스토리지에 파일 제거
     * @param filepath 파일 경로 eg. /users/user/filename.png
     */
    void removeFile(String filepath);
}
