package com.flyby.ramble.report.service;

import com.flyby.ramble.common.producer.MessageProducer;
import com.flyby.ramble.common.service.StorageService;
import com.flyby.ramble.report.dto.DetectNudeCommandDTO;
import com.flyby.ramble.report.dto.EncryptedSnapshotUploadResultDTO;
import com.flyby.ramble.report.dto.NudeDetectionRequestDTO;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static com.flyby.ramble.report.constants.Constants.MESSAGE_TOPIC_NUDE_DETECTION_REQUEST;
import static com.flyby.ramble.report.constants.Constants.STORAGE_REPORT_DIR;

@Service
@RequiredArgsConstructor
@Slf4j
public class NudeDetectionServiceImpl implements NudeDetectionService {
    private final MessageProducer messageProducer;
    private final StorageService storageService;

    public void requestDetection(DetectNudeCommandDTO commandDTO) {
        EncryptedSnapshotUploadResultDTO resultDTO = gzipEncryptAndUploadSnapshot(commandDTO.getPeerVideoSnapshot());

        messageProducer.send(
                MESSAGE_TOPIC_NUDE_DETECTION_REQUEST,
                NudeDetectionRequestDTO.builder()
                        .reportId(commandDTO.getReportUuid())
                        .fileUrl(resultDTO.getFileUrl())
                        .keyUrl(resultDTO.getKeyUrl())
                        .build()
        );
    }

    private EncryptedSnapshotUploadResultDTO gzipEncryptAndUploadSnapshot(MultipartFile videoSnapshot) {
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
            String fileUrl = urlPrefix + "/" + uuid + ".enc.gz";
            String keyUrl = urlPrefix + "/" + uuid + ".key.enc";

            // 임시 파일 생성
            tempFile = File.createTempFile("snapshot-", ".enc.gz");

            try (
                    InputStream inputStream = videoSnapshot.getInputStream();
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    CipherOutputStream cipherOut = new CipherOutputStream(fos, aesCipher);
                    GZIPOutputStream gzipOut = new GZIPOutputStream(cipherOut)
            ) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    gzipOut.write(buffer, 0, bytesRead);
                }
                gzipOut.finish();
            }

            long contentLength = tempFile.length();

            try (InputStream fileInput = new FileInputStream(tempFile)) {
                storageService.uploadFile(fileUrl, fileInput, contentLength, "application/octet-stream");
            }

            KeyPair rsaKeyPair = getRSAKeyPair();
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
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
                tempFile.delete();
            }
        }
    }

    private KeyPair getRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
}
