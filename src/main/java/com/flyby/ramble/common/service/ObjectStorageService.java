package com.flyby.ramble.common.service;

import com.flyby.ramble.common.config.MinioConfig;
import com.flyby.ramble.common.exception.StorageOperationException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageService implements StorageService {
    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    @Override
    public void uploadFile(String newFilePath, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(newFilePath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (IOException | XmlParserException | InternalException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException | ErrorResponseException | InsufficientDataException | ServerException e) {
            String message = "cannot upload file. Error message: " + e.getMessage();
            log.error(message);
            throw new StorageOperationException(message);
        }
    }

    @Override
    public void uploadFile(String newFilePath, byte[] fileBytes, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(newFilePath)
                            .stream(inputStream, fileBytes.length, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (IOException | XmlParserException | InternalException | InvalidResponseException |
                 InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException |
                 InsufficientDataException | ServerException e) {

            String message = "Cannot upload file (byte[]). Error message: " + e.getMessage();
            log.error(message);
            throw new StorageOperationException(message);
        }
    }

    @Override
    public void uploadFile(String newFilePath, InputStream inputStream, long contentLength, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(newFilePath)
                            .stream(inputStream, contentLength, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (IOException | XmlParserException | InternalException | InvalidResponseException |
                 InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException |
                 InsufficientDataException | ServerException e) {

            String message = "Cannot upload file (stream). Error message: " + e.getMessage();
            log.error(message);
            throw new StorageOperationException(message);
        }
    }

    @Override
    public void removeFile(String filepath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(filepath)
                            .bypassGovernanceMode(true)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            String message = "Cannot remove file. Error message: " + e.getMessage();
            log.warn(message);
            throw new StorageOperationException(message);
        }
    }
}
