package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.auth.command.application.port.AdminAuthPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 인증 요청 제한과 비밀번호 재설정 설정이다.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AdminAuthProperties(
        RequestPolicy emailVerification,
        PasswordReset passwordReset
) implements AdminAuthPolicy {

    @Override
    public AdminAuthPolicy.RequestLimit emailVerificationPolicy() {
        return new AdminAuthPolicy.RequestLimit(
                emailVerification.requestLimit(),
                emailVerification.requestWindow(),
                emailVerification.resendCooldown()
        );
    }

    @Override
    public AdminAuthPolicy.PasswordReset passwordResetPolicy() {
        return new AdminAuthPolicy.PasswordReset(
                passwordReset.requestLimit(),
                passwordReset.requestWindow(),
                passwordReset.resendCooldown(),
                passwordReset.tokenTtl(),
                passwordReset.frontendUrl()
        );
    }

    public record RequestPolicy(
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown
    ) {
        public RequestPolicy {
            validateRequestLimit(
                    requestLimit,
                    requestWindow,
                    resendCooldown
            );
        }
    }

    public record PasswordReset(
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown,
            Duration tokenTtl,
            String frontendUrl
    ) {
        public PasswordReset {
            validateRequestLimit(
                    requestLimit,
                    requestWindow,
                    resendCooldown
            );
            if (tokenTtl == null
                    || tokenTtl.isZero()
                    || tokenTtl.isNegative()
                    || frontendUrl == null
                    || frontendUrl.isBlank()) {
                throw new IllegalArgumentException(
                        "비밀번호 재설정 TTL과 프런트 URL은 필수입니다."
                );
            }
        }
    }

    private static void validateRequestLimit(
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown
    ) {
        if (requestLimit <= 0
                || requestWindow == null
                || requestWindow.isZero()
                || requestWindow.isNegative()
                || resendCooldown == null
                || resendCooldown.isZero()
                || resendCooldown.isNegative()
                || resendCooldown.compareTo(requestWindow) > 0) {
            throw new IllegalArgumentException(
                    "인증 요청 횟수와 제한 시간은 양수여야 합니다."
            );
        }
    }
}
