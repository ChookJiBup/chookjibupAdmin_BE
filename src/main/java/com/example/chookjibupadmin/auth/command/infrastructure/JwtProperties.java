package com.example.chookjibupadmin.auth.command.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 발급과 검증에 사용하는 설정값이다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationSeconds
) {
}
