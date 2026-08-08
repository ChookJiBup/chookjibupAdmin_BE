package com.example.chookjibupadmin.auth.command.application.port;

import java.time.Duration;

/**
 * 관리자 인증 요청 제한과 비밀번호 재설정 정책을 제공한다.
 */
public interface AdminAuthPolicy {

    RequestLimit emailVerificationPolicy();

    PasswordReset passwordResetPolicy();

    record RequestLimit(
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown
    ) {
    }

    record PasswordReset(
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown,
            Duration tokenTtl,
            String frontendUrl
    ) {
    }
}
