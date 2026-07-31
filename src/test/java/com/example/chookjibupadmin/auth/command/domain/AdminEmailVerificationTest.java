package com.example.chookjibupadmin.auth.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminEmailVerificationTest {

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("이메일 인증 요청을 미인증 상태로 생성한다")
        void success_Issue() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);

            // when
            AdminEmailVerification verification = AdminEmailVerification.issue(
                    email,
                    "123456",
                    expiresAt
            );

            // then
            assertThat(verification.getEmail()).isEqualTo(email);
            assertThat(verification.getCode()).isEqualTo("123456");
            assertThat(verification.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(verification.isVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("저장된 인증 완료 상태를 복원한다")
        void success_Restore_Verified() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);

            // when
            AdminEmailVerification verification = AdminEmailVerification.restore(
                    email,
                    "123456",
                    expiresAt,
                    true
            );

            // then
            assertThat(verification.isVerified()).isTrue();
            assertThat(verification.getEmail()).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("만료 시각과 같은 시각에는 인증할 수 있다")
        void success_Verify_ExpiresAtBoundary() {
            // given
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);
            AdminEmailVerification verification = verification(expiresAt);

            // when
            verification.verify("123456", expiresAt);

            // then
            assertThat(verification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("만료 시각 이후에는 인증할 수 없다")
        void fail_Verify_Expired_CustomException() {
            // given
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);
            AdminEmailVerification verification = verification(expiresAt);

            // when & then
            assertThatThrownBy(() -> verification.verify(
                    "123456",
                    expiresAt.plusNanos(1)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED
                                    .getMessage()
                    );
        }

        @Test
        @DisplayName("인증 코드가 일치하지 않으면 인증할 수 없다")
        void fail_Verify_InvalidCode_CustomException() {
            // given
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);
            AdminEmailVerification verification = verification(expiresAt);

            // when & then
            assertThatThrownBy(() -> verification.verify(
                    "000000",
                    expiresAt.minusSeconds(1)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID
                                    .getMessage()
                    );
        }
    }

    private AdminEmailVerification verification(LocalDateTime expiresAt) {
        return AdminEmailVerification.issue(
                AdminEmail.of("admin@mapo.go.kr"),
                "123456",
                expiresAt
        );
    }
}
