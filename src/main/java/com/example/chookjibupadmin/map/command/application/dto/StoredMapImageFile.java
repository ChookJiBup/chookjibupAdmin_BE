package com.example.chookjibupadmin.map.command.application.dto;

import java.nio.file.Path;

/**
 * Storage Adapter에 전달할 검증 완료 이미지 파일이다.
 */
public record StoredMapImageFile(
        String objectKey,
        Path path,
        long contentLength,
        String contentType,
        String checksumSha256
) {
}
