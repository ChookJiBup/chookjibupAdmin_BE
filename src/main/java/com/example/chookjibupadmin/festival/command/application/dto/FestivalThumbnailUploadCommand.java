package com.example.chookjibupadmin.festival.command.application.dto;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP 기술 타입과 분리된 축제 대표 썸네일 업로드 입력이다.
 */
public record FestivalThumbnailUploadCommand(
        String originalFileName,
        String declaredContentType,
        long fileSize,
        InputStreamSupplier inputStreamSupplier
) {

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }
}
