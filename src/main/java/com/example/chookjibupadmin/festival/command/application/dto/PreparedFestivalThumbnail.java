package com.example.chookjibupadmin.festival.command.application.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 검증과 크기 정규화를 마친 축제 대표 썸네일이다.
 */
public record PreparedFestivalThumbnail(
        Path path,
        long fileSize,
        String contentType,
        String checksumSha256,
        int width,
        int height
) implements AutoCloseable {

    @Override
    public void close() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 삭제 실패는 원래 요청 결과를 변경하지 않는다.
        }
    }
}
