package com.example.chookjibupadmin.festival.command.infrastructure.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 축제 대표 썸네일의 업로드 및 변환 제한을 관리한다.
 */
@ConfigurationProperties(prefix = "app.festival.thumbnail")
public record FestivalThumbnailProperties(
        DataSize maxFileSize,
        int maxOriginalSide,
        long maxOriginalPixels,
        int maxSide,
        double jpegQuality
) {
}
