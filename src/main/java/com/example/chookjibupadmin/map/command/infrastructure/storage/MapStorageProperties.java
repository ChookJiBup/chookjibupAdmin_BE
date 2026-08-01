package com.example.chookjibupadmin.map.command.infrastructure.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 축제 배치도 S3 저장 설정을 관리한다.
 */
@ConfigurationProperties(prefix = "app.map.storage")
public record MapStorageProperties(
        String provider,
        String bucket,
        String region,
        URI endpoint,
        boolean pathStyleAccessEnabled,
        Duration connectTimeout,
        Duration apiCallTimeout
) {

    public boolean isS3() {
        return "s3".equalsIgnoreCase(provider);
    }

    public boolean hasBucket() {
        return bucket != null && !bucket.isBlank();
    }
}
