package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoggingAdminEmailVerificationSenderTest {

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("임시 이메일 인증 코드를 로그로 발송 처리한다")
        void success_Send() {
            // given
            LoggingAdminEmailVerificationSender sender =
                    new LoggingAdminEmailVerificationSender();
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> sender.send(email, "123456"));
        }
    }
}
