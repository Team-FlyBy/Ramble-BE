package com.flyby.ramble.report.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EncryptedSnapshotUploadResultDTO {
    private String fileUrl;
    private String keyUrl;
}
