package com.example.chookjibupadmin.map.command.application.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 검증된 축제 도면 원본과 화면 표시용·AI 분석용 파생 이미지이다.
 */
public record PreparedMapImage(
        String originalFileName,
        Path originalPath,
        Path displayPath,
        Path analysisPath,
        String originalContentType,
        String displayContentType,
        String analysisContentType,
        String originalExtension,
        String displayExtension,
        String analysisExtension,
        long originalFileSize,
        long displayFileSize,
        long analysisFileSize,
        int displayImageWidth,
        int displayImageHeight,
        int analysisImageWidth,
        int analysisImageHeight,
        String originalChecksumSha256,
        String displayChecksumSha256,
        String analysisChecksumSha256
) implements AutoCloseable {

    @Override
    public void close() {
        deleteIfExists(originalPath);
        deleteIfExists(displayPath);
        deleteIfExists(analysisPath);
    }

    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 삭제 실패는 원래 요청 결과를 변경하지 않는다.
        }
    }
}
