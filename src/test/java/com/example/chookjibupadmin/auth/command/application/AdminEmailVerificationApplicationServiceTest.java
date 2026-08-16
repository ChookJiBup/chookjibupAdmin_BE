package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationConfirmRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationRequest;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerificationRepository;
import com.example.chookjibupadmin.auth.command.infrastructure.AdminAuthProperties;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminEmailVerificationApplicationServiceTest {

    @Mock
    private AdminAccountService adminAccountService;

    private FakeAdminEmailVerificationRepository verificationRepository;
    private AdminEmailVerificationService adminEmailVerificationService;
    private RecordingEmailVerificationSender verificationSender;
    private AdminEmailVerificationApplicationService verificationService;

    @Mock
    private AdminAuthRequestLimiter requestLimiter;

    @BeforeEach
    void setUp() {
        verificationRepository = new FakeAdminEmailVerificationRepository();
        adminEmailVerificationService = new AdminEmailVerificationService(
                verificationRepository
        );
        verificationSender = new RecordingEmailVerificationSender();
        verificationService = new AdminEmailVerificationApplicationService(
                adminAccountService,
                adminEmailVerificationService,
                verificationSender,
                requestLimiter,
                new AdminAuthProperties(
                        new AdminAuthProperties.RequestPolicy(
                                5,
                                Duration.ofMinutes(10),
                                Duration.ofMinutes(1)
                        ),
                        new AdminAuthProperties.PasswordReset(
                                5,
                                Duration.ofMinutes(10),
                                Duration.ofMinutes(1),
                                Duration.ofMinutes(30),
                                "http://localhost:3000/reset-password"
                        )
                )
        );
        lenient().when(requestLimiter.tryAcquire(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);
    }

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        @DisplayName("정부 이메일이면 6자리 인증 코드를 발급한다")
        void success_Request_GovernmentEmail() {
            // given
            String email = "admin@mapo.go.kr";
            given(adminAccountService.existsByEmail(AdminEmail.of(email)))
                    .willReturn(false);

            // when
            verificationService.request(new AdminEmailVerificationRequest(email));

            // then
            assertThat(verificationSender.email).isEqualTo(email);
            assertThat(verificationSender.code).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 인증 코드를 발급할 수 없다")
        void fail_Request_DuplicatedEmail_CustomException() {
            // given
            String email = "admin@mapo.go.kr";
            given(adminAccountService.existsByEmail(AdminEmail.of(email)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> verificationService.request(
                    new AdminEmailVerificationRequest(email)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_DUPLICATED.getMessage());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("발급된 인증 코드가 일치하면 인증 완료 상태가 된다")
        void success_Confirm_MatchedCode() {
            // given
            String email = "admin@mapo.go.kr";
            given(adminAccountService.existsByEmail(AdminEmail.of(email)))
                    .willReturn(false);
            verificationService.request(new AdminEmailVerificationRequest(email));

            // when
            verificationService.confirm(new AdminEmailVerificationConfirmRequest(
                    email,
                    verificationSender.code
            ));

            // then
            assertThat(verificationRepository.consumeVerified(AdminEmail.of(email)))
                    .isTrue();
        }

        @Test
        @DisplayName("인증 요청이 없으면 인증 코드를 확인할 수 없다")
        void fail_Confirm_NotFound_CustomException() {
            // given
            AdminEmailVerificationConfirmRequest request =
                    new AdminEmailVerificationConfirmRequest(
                            "admin@mapo.go.kr",
                            "123456"
                    );

            // when & then
            assertThatThrownBy(() -> verificationService.confirm(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_VERIFICATION_NOT_FOUND
                                    .getMessage()
                    );
        }

        @Test
        @DisplayName("인증 코드가 일치하지 않으면 인증할 수 없다")
        void fail_Confirm_InvalidCode_CustomException() {
            // given
            String email = "admin@mapo.go.kr";
            given(adminAccountService.existsByEmail(AdminEmail.of(email)))
                    .willReturn(false);
            verificationService.request(new AdminEmailVerificationRequest(email));

            // when & then
            assertThatThrownBy(() -> verificationService.confirm(
                    new AdminEmailVerificationConfirmRequest(email, "000000")
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID
                                    .getMessage()
                    );
        }

        @Test
        @DisplayName("인증 코드 실패가 5회 누적되면 올바른 코드도 거부한다")
        void fail_Confirm_MaxFailedAttempts_CustomException() {
            // given
            String email = "admin@mapo.go.kr";
            given(adminAccountService.existsByEmail(AdminEmail.of(email)))
                    .willReturn(false);
            verificationService.request(new AdminEmailVerificationRequest(email));
            String validCode = verificationSender.code;
            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> verificationService.confirm(
                        new AdminEmailVerificationConfirmRequest(email, "invalid")
                )).isInstanceOf(CustomException.class);
            }

            // when & then
            assertThatThrownBy(() -> verificationService.confirm(
                    new AdminEmailVerificationConfirmRequest(email, validCode)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID
                                    .getMessage()
                    );
        }
    }

    @Nested
    @DisplayName("consumeVerified")
    class ConsumeVerified {

        @Test
        @DisplayName("인증 완료 이메일은 한 번만 소비할 수 있다")
        void success_ConsumeVerified_OnlyOnce() {
            // given
            String value = "admin@mapo.go.kr";
            AdminEmail email = AdminEmail.of(value);
            given(adminAccountService.existsByEmail(email)).willReturn(false);
            verificationService.request(new AdminEmailVerificationRequest(value));
            verificationService.confirm(new AdminEmailVerificationConfirmRequest(
                    value,
                    verificationSender.code
            ));

            // when
            adminEmailVerificationService.consumeVerified(email);

            // then
            assertThatThrownBy(() -> adminEmailVerificationService.consumeVerified(email))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_NOT_VERIFIED.getMessage());
        }

        @Test
        @DisplayName("인증 완료 상태가 없으면 소비할 수 없다")
        void fail_ConsumeVerified_NotVerified_CustomException() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");

            // when & then
            assertThatThrownBy(() -> adminEmailVerificationService.consumeVerified(email))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_NOT_VERIFIED.getMessage());
        }
    }

    private static class RecordingEmailVerificationSender
            implements AdminEmailVerificationSender {

        private String email;
        private String code;

        @Override
        public void send(AdminEmail email, String code) {
            this.email = email.getValue();
            this.code = code;
        }
    }

    private static class FakeAdminEmailVerificationRepository
            implements AdminEmailVerificationRepository {

        private final Map<String, AdminEmailVerification> storage =
                new ConcurrentHashMap<>();

        @Override
        public void save(AdminEmailVerification verification) {
            storage.put(verification.getEmail().getValue(), verification);
        }

        @Override
        public Optional<AdminEmailVerification> findByEmail(AdminEmail email) {
            return Optional.ofNullable(storage.get(email.getValue()));
        }

        @Override
        public void deleteByEmail(AdminEmail email) {
            storage.remove(email.getValue());
        }

        @Override
        public boolean consumeVerified(AdminEmail email) {
            AdminEmailVerification verification = storage.get(email.getValue());
            if (verification == null || !verification.isVerified()) {
                return false;
            }

            storage.remove(email.getValue());
            return true;
        }
    }
}
