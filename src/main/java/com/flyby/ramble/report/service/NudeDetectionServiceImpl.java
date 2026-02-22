package com.flyby.ramble.report.service;

import com.flyby.ramble.common.producer.MessageProducer;
import com.flyby.ramble.common.service.StorageService;
import com.flyby.ramble.report.config.UserSnapshotEncryptionProperties;
import com.flyby.ramble.report.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

import static com.flyby.ramble.report.constants.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NudeDetectionServiceImpl implements NudeDetectionService {
    private final UserSnapshotEncryptionProperties userSnapshotEncryptionProperties;
    private final MessageProducer messageProducer;
    private final StorageService storageService;

    @Override
    public void requestDetection(DetectNudeCommandDTO commandDTO) {
        EncryptedSnapshotUploadResultDTO resultDTO = encryptAndUploadSnapshot(commandDTO.getPeerVideoSnapshot());

        messageProducer.send(
                MESSAGE_TOPIC_NUDE_DETECTION_REQUEST,
                NudeDetectionRequestedEventDTO.builder()
                        .reportUuid(commandDTO.getReportUuid())
                        .fileUrl(resultDTO.getFileUrl())
                        .keyUrl(resultDTO.getKeyUrl())
                        .build()
        );
    }

    @Override
    public void requestAutoDetection(AutoNudeDetectionCommandDTO commandDTO) {
        EncryptedSnapshotUploadResultDTO resultDTO = encryptAndUploadSnapshot(commandDTO.getPeerVideoSnapshot());

        messageProducer.send(
                MESSAGE_TOPIC_AUTO_NUDE_DETECTION_REQUEST,
                AutoNudeDetectionRequestedEventDTO.builder()
                        .userUuid(commandDTO.getUserUuid())
                        .fileUrl(resultDTO.getFileUrl())
                        .keyUrl(resultDTO.getKeyUrl())
                        .build()
        );
    }

    private EncryptedSnapshotUploadResultDTO encryptAndUploadSnapshot(MultipartFile videoSnapshot) {
        File tempFile = null;

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey aesKey = keyGenerator.generateKey();

            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);

            String uuid = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            String urlPrefix = STORAGE_REPORT_DIR + "/" + now.format(DateTimeFormatter.BASIC_ISO_DATE);
            String fileUrl = urlPrefix + "/" + uuid + ".jpg.enc";
            String keyUrl = urlPrefix + "/" + uuid + ".key.enc";

            // 임시 파일 생성
            tempFile = File.createTempFile("snapshot-", ".jpg.enc");
            tempFile.setReadable(false, false);
            tempFile.setReadable(true, true);
            tempFile.setWritable(false, false);
            tempFile.setWritable(true, true);

            try (
                    InputStream inputStream = videoSnapshot.getInputStream();
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    CipherOutputStream cipherOut = new CipherOutputStream(fos, aesCipher);
            ) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    cipherOut.write(buffer, 0, bytesRead);
                }
                cipherOut.flush();
            }

            long contentLength = tempFile.length();

            try (InputStream fileInput = new FileInputStream(tempFile)) {
                storageService.uploadFile(fileUrl, fileInput, contentLength, "application/octet-stream");
            }

            PublicKey publicKey = getPublicKeyFromBase64(userSnapshotEncryptionProperties.getPublicKey());
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            storageService.uploadFile(keyUrl, encryptedAesKey, "application/octet-stream");

            return EncryptedSnapshotUploadResultDTO.builder()
                    .fileUrl(fileUrl)
                    .keyUrl(keyUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to upload encrypted snapshot... {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    // Base64 공개키 문자열을 PublicKey 객체로 변환
    private PublicKey getPublicKeyFromBase64(String base64PublicKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
