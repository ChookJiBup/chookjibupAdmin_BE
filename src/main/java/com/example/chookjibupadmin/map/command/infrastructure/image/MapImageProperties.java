package com.example.chookjibupadmin.map.command.infrastructure.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * AI 분석용 축제 도면 이미지의 업로드 및 파생 이미지 제한을 관리한다.
 */
@ConfigurationProperties(prefix = "app.map.image")
public record MapImageProperties(
        DataSize maxFileSize,
        int minWidth,
        int minHeight,
        int maxOriginalSide,
        long maxOriginalPixels,
        int analysisMaxSide,
        double analysisJpegQuality
) {
}
