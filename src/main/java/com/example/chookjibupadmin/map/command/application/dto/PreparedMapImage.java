package com.example.chookjibupadmin.map.command.application.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 검증된 업로드 원본과 화면 표시용 정규화 이미지이다.
 */
public record PreparedMapImage(
        String originalFileName,
        Path sourcePath,
        Path displayPath,
        String sourceContentType,
        String displayContentType,
        String sourceExtension,
        String displayExtension,
        long sourceFileSize,
        long displayFileSize,
        int imageWidth,
        int imageHeight,
        String sourceChecksumSha256,
        String displayChecksumSha256
) implements AutoCloseable {

    @Override
    public void close() {
        deleteIfExists(sourcePath);
        deleteIfExists(displayPath);
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
