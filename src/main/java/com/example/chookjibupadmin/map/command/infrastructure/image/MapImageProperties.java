package com.example.chookjibupadmin.map.command.infrastructure.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 배치도 이미지 검증 제한을 관리한다.
 */
@ConfigurationProperties(prefix = "app.map.image")
public record MapImageProperties(
        DataSize maxFileSize,
        int minWidth,
        int minHeight,
        int maxSide,
        long maxPixels
) {
}
