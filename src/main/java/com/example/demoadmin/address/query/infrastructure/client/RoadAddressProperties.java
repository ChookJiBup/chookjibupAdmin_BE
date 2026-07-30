package com.example.demoadmin.address.query.infrastructure.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공식 도로명주소 검색 API 접속 설정을 관리한다.
 */
@ConfigurationProperties(prefix = "app.road-address")
public record RoadAddressProperties(
        String baseUrl,
        String confirmationKey,
        Duration connectTimeout,
        Duration readTimeout
) {

    /**
     * 실제 주소 검색 승인키가 설정되었는지 확인한다.
     */
    public boolean hasConfirmationKey() {
        return confirmationKey != null && !confirmationKey.isBlank();
    }
}
