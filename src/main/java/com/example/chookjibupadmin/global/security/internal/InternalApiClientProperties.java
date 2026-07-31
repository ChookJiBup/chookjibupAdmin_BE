package com.example.chookjibupadmin.global.security.internal;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서버 간 internal API 인증에 사용할 client secret과 시간 정책을 보관한다.
 */
@ConfigurationProperties(prefix = "app.internal-auth")
public record InternalApiClientProperties(
        Map<String, Client> clients,
        Duration timestampTolerance,
        Duration nonceTtl
) {

    private static final Duration DEFAULT_TIMESTAMP_TOLERANCE =
            Duration.ofMinutes(5);
    private static final Duration DEFAULT_NONCE_TTL = Duration.ofMinutes(10);

    public InternalApiClientProperties {
        clients = clients == null ? Map.of() : Map.copyOf(clients);
        timestampTolerance = timestampTolerance == null
                ? DEFAULT_TIMESTAMP_TOLERANCE
                : timestampTolerance;
        nonceTtl = nonceTtl == null ? DEFAULT_NONCE_TTL : nonceTtl;
    }

    /**
     * 특정 internal client의 active/previous secret 설정이다.
     */
    public record Client(
            String secret,
            String previousSecret
    ) {
    }
}
