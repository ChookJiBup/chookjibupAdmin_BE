package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.api.auth.dto.AdminPasswordResetConfirmRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminPasswordResetRequest;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetEmailSender;
import com.example.chookjibupadmin.auth.command.infrastructure.AdminAuthProperties;
import com.example.chookjibupadmin.auth.command.infrastructure.AdminPasswordResetTokenManager;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminPasswordResetApplicationServiceTest {

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminPasswordResetTokenService tokenService;

    @Mock
    private AdminPasswordResetTokenManager tokenManager;

    @Mock
    private AdminPasswordResetEmailSender emailSender;

    @Mock
    private AdminAuthRequestLimiter requestLimiter;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminPasswordResetApplicationService service;

    @BeforeEach
    void setUp() {
        AdminAuthProperties properties = new AdminAuthProperties(
                null,
                new AdminAuthProperties.PasswordReset(
                        5,
                        Duration.ofMinutes(10),
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(30),
                        "https://admin.chookjibup.store/reset-password"
                )
        );
        service = new AdminPasswordResetApplicationService(
                adminAccountService,
                tokenService,
                tokenManager,
                emailSender,
                requestLimiter,
                properties,
                passwordEncoder
        );
    }

    @Nested
    @DisplayName("requestForAuthenticatedAdmin")
    class RequestForAuthenticatedAdmin {

        @Test
        @DisplayName("로그인 계정의 등록 이메일로 비밀번호 변경 링크를 발송한다")
        void success_RequestForAuthenticatedAdmin_ActiveAccount() {
            // given
            AdminAccount account = adminAccount();
            given(adminAccountService.getById(1L)).willReturn(account);
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(true);
            given(tokenManager.generate()).willReturn("raw-token");
            given(tokenManager.hash("raw-token")).willReturn("token-hash");

            // when
            service.requestForAuthenticatedAdmin(
                    new AdminPrincipal(1L, "jwt-email@mapo.go.kr")
            );

            // then
            then(requestLimiter).should().tryAcquire(
                    "password-reset",
                    account.getEmail(),
                    5,
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(1)
            );
            then(tokenService).should().save(
                    1L,
                    "token-hash",
                    Duration.ofMinutes(30)
            );
            then(emailSender).should().send(
                    account.getEmail(),
                    "https://admin.chookjibup.store/reset-password?token=raw-token"
            );
        }

        @Test
        @DisplayName("인증 주체가 없으면 링크를 발급하지 않는다")
        void fail_RequestForAuthenticatedAdmin_MissingPrincipal_CustomException() {
            assertThatThrownBy(() -> service.requestForAuthenticatedAdmin(null))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
            then(emailSender).shouldHaveNoInteractions();
            then(tokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("비활성 계정이면 링크를 발급하지 않는다")
        void fail_RequestForAuthenticatedAdmin_InactiveAccount_CustomException() {
            // given
            AdminAccount account = adminAccount();
            account.withdraw();
            given(adminAccountService.getById(1L)).willReturn(account);

            // when & then
            assertThatThrownBy(() -> service.requestForAuthenticatedAdmin(
                    new AdminPrincipal(1L, account.getEmailValue())
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
            then(emailSender).shouldHaveNoInteractions();
            then(tokenService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        @DisplayName("활성 계정이면 해시 토큰을 저장하고 원문 토큰이 포함된 링크를 발송한다")
        void success_Request_ActiveAccount() {
            // given
            AdminAccount account = adminAccount();
            AdminEmail email = account.getEmail();
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(true);
            given(adminAccountService.findByEmail(email))
                    .willReturn(Optional.of(account));
            given(tokenManager.generate()).willReturn("raw-token");
            given(tokenManager.hash("raw-token")).willReturn("token-hash");

            // when
            service.request(new AdminPasswordResetRequest(email.getValue()));

            // then
            then(tokenService).should().save(
                    1L,
                    "token-hash",
                    Duration.ofMinutes(30)
            );
            then(emailSender).should().send(
                    email,
                    "https://admin.chookjibup.store/reset-password?token=raw-token"
            );
        }

        @Test
        @DisplayName("외부업자 일반 이메일이면 비밀번호 재설정 링크를 발송한다")
        void success_Request_ContractorEmail() {
            // given
            AdminAccount account = contractorAccount();
            AdminEmail email = account.getEmail();
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(true);
            given(adminAccountService.findByEmail(email))
                    .willReturn(Optional.of(account));
            given(tokenManager.generate()).willReturn("raw-token");
            given(tokenManager.hash("raw-token")).willReturn("token-hash");

            // when
            service.request(new AdminPasswordResetRequest(email.getValue()));

            // then
            then(tokenService).should().save(
                    2L,
                    "token-hash",
                    Duration.ofMinutes(30)
            );
            then(emailSender).should().send(
                    email,
                    "https://admin.chookjibup.store/reset-password?token=raw-token"
            );
        }

        @Test
        @DisplayName("등록되지 않은 이메일도 성공 처리하되 메일은 발송하지 않는다")
        void success_Request_UnknownAccountBoundary() {
            // given
            AdminEmail email = AdminEmail.of("unknown@mapo.go.kr");
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(true);
            given(adminAccountService.findByEmail(email)).willReturn(Optional.empty());

            // when
            service.request(new AdminPasswordResetRequest(email.getValue()));

            // then
            then(emailSender).shouldHaveNoInteractions();
            then(tokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("메일 발송 실패 시 발급한 토큰을 제거한다")
        void fail_Request_MailFailure_CustomException() {
            // given
            AdminAccount account = adminAccount();
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(true);
            given(adminAccountService.findByEmail(account.getEmail()))
                    .willReturn(Optional.of(account));
            given(tokenManager.generate()).willReturn("raw-token");
            given(tokenManager.hash("raw-token")).willReturn("token-hash");
            org.mockito.BDDMockito.willThrow(new IllegalStateException("smtp failure"))
                    .given(emailSender)
                    .send(any(), any());

            // when & then
            assertThatThrownBy(() -> service.request(
                    new AdminPasswordResetRequest(account.getEmailValue())
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_SEND_FAILED.getMessage());
            then(tokenService).should().delete(1L, "token-hash");
        }

        @Test
        @DisplayName("재요청 제한을 넘으면 링크를 발급하지 않는다")
        void fail_Request_RateLimited_CustomException() {
            // given
            given(requestLimiter.tryAcquire(any(), any(), any(Integer.class), any(), any()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> service.request(
                    new AdminPasswordResetRequest("admin@mapo.go.kr")
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_EMAIL_REQUEST_LIMIT_EXCEEDED.getMessage()
                    );
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("유효한 토큰이면 새 비밀번호 해시를 저장한다")
        void success_Confirm_ValidToken() {
            // given
            AdminAccount account = adminAccount();
            given(tokenManager.hash("raw-token")).willReturn("token-hash");
            given(tokenService.consume("token-hash")).willReturn(1L);
            given(adminAccountService.getById(1L)).willReturn(account);
            given(passwordEncoder.encode("NewPassword!123"))
                    .willReturn("new-password-hash");

            // when
            service.confirm(new AdminPasswordResetConfirmRequest(
                    "raw-token",
                    "NewPassword!123",
                    "NewPassword!123"
            ));

            // then
            ArgumentCaptor<AdminPasswordHash> hashCaptor =
                    ArgumentCaptor.forClass(AdminPasswordHash.class);
            then(adminAccountService).should().changePassword(
                    org.mockito.ArgumentMatchers.eq(account),
                    hashCaptor.capture()
            );
            org.assertj.core.api.Assertions.assertThat(hashCaptor.getValue().getValue())
                    .isEqualTo("new-password-hash");
        }

        @Test
        @DisplayName("비밀번호 확인이 다르면 토큰을 소비하지 않는다")
        void fail_Confirm_PasswordMismatch_CustomException() {
            // when & then
            assertThatThrownBy(() -> service.confirm(
                    new AdminPasswordResetConfirmRequest(
                            "raw-token",
                            "NewPassword!123",
                            "OtherPassword!123"
                    )
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH.getMessage());
            then(tokenService).shouldHaveNoInteractions();
        }
    }

    private AdminAccount adminAccount() {
        AdminAccount account = AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("old-password-hash")
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }

    private AdminAccount contractorAccount() {
        AdminAccount account = AdminAccount.createContractor(
                AdminEmail.of("vendor@gmail.com"),
                AdminName.of("김업체"),
                AdminOrganization.of("축제기획(주)"),
                AdminPasswordHash.of("old-password-hash")
        );
        ReflectionTestUtils.setField(account, "id", 2L);
        return account;
    }
}
