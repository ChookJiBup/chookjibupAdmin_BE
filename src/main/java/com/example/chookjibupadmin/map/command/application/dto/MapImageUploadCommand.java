package com.example.chookjibupadmin.map.command.application.dto;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP 기술 타입과 분리된 배치도 이미지 업로드 입력이다.
 */
public record MapImageUploadCommand(
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
