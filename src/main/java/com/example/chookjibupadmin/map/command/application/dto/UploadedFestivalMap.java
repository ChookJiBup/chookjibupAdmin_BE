package com.example.chookjibupadmin.map.command.application.dto;

import java.util.UUID;

/**
 * S3 저장이 끝난 배치도 메타데이터를 DB 트랜잭션에 전달한다.
 */
public record UploadedFestivalMap(
        UUID publicId,
        String mapName,
        String originalFileName,
        String sourceImageKey,
        String displayImageKey,
        String sourceContentType,
        String displayContentType,
        long sourceFileSize,
        long displayFileSize,
        int imageWidth,
        int imageHeight,
        String sourceChecksumSha256,
        String displayChecksumSha256
) {
}
