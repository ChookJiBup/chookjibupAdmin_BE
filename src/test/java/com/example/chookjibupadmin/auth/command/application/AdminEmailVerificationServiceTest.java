package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerificationRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminEmailVerificationServiceTest {

    @InjectMocks
    private AdminEmailVerificationService verificationService;

    @Mock
    private AdminEmailVerificationRepository verificationRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("이메일 인증 요청 정보를 저장한다")
        void success_Save() {
            // given
            AdminEmailVerification verification = verification();

            // when
            verificationService.save(verification);

            // then
            then(verificationRepository).should().save(verification);
        }
    }

    @Nested
    @DisplayName("getByEmail")
    class GetByEmail {

        @Test
        @DisplayName("이메일로 인증 요청 정보를 조회한다")
        void success_GetByEmail() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            AdminEmailVerification verification = verification();
            given(verificationRepository.findByEmail(email))
                    .willReturn(Optional.of(verification));

            // when
            AdminEmailVerification found = verificationService.getByEmail(email);

            // then
            assertThat(found).isSameAs(verification);
        }

        @Test
        @DisplayName("인증 요청 정보가 없으면 예외를 던진다")
        void fail_GetByEmail_CustomException() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            given(verificationRepository.findByEmail(email))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verificationService.getByEmail(email))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_VERIFICATION_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("ensureVerified")
    class EnsureVerified {

        @Test
        @DisplayName("인증 종류가 다르면 가입할 수 없다")
        void fail_EnsureVerified_AccountKindMismatch() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            given(verificationRepository.findByEmail(email))
                    .willReturn(Optional.of(verified(AccountKind.GOVERNMENT)));

            // when & then
            assertThatThrownBy(() -> verificationService.ensureVerified(
                    email,
                    AccountKind.CONTRACTOR
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_NOT_VERIFIED.getMessage());
        }
    }

    private AdminEmailVerification verification() {
        return AdminEmailVerification.issue(
                AdminEmail.of("admin@mapo.go.kr"),
                AccountKind.GOVERNMENT,
                "123456",
                LocalDateTime.now().plusMinutes(5)
        );
    }

    private AdminEmailVerification verified(AccountKind accountKind) {
        AdminEmailVerification verification = AdminEmailVerification.issue(
                AdminEmail.of("admin@mapo.go.kr"),
                accountKind,
                "123456",
                LocalDateTime.now().plusMinutes(5)
        );
        verification.verify("123456", LocalDateTime.now());
        return verification;
    }
}
