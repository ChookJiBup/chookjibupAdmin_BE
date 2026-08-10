package com.example.chookjibupadmin.operator.command.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 현장 스태프 JWT 발급과 검증에 사용하는 설정값이다.
 */
@ConfigurationProperties(prefix = "app.field-staff.jwt")
public record FieldStaffJwtProperties(
        String secret,
        long accessTokenExpirationSeconds
) {
}
