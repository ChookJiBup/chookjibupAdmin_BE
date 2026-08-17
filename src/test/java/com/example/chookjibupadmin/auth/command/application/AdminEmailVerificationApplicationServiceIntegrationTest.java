package com.example.chookjibupadmin.auth.command.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationRequest;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AdminEmailVerificationApplicationServiceIntegrationTest {

    @Autowired
    private AdminEmailVerificationApplicationService emailVerificationService;

    @MockitoBean
    private AdminAccountService adminAccountService;

    @MockitoBean
    private AdminEmailVerificationService verificationService;

    @MockitoBean
    private AdminEmailVerificationSender verificationSender;

    @MockitoBean
    private AdminAuthRequestLimiter requestLimiter;

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        @DisplayName("이메일 인증 요청을 저장하고 인증 코드를 발송한다")
        void success_Request() {
            // given
            String email = "admin@mapo.go.kr";
            AdminEmail adminEmail = AdminEmail.of(email);
            given(adminAccountService.existsByEmail(adminEmail))
                    .willReturn(false);
            given(requestLimiter.tryAcquire(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.eq(adminEmail),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()
            )).willReturn(true);

            // when
            emailVerificationService.request(
                    new AdminEmailVerificationRequest(email, AccountKind.GOVERNMENT)
            );

            // then
            ArgumentCaptor<AdminEmailVerification> captor =
                    ArgumentCaptor.forClass(AdminEmailVerification.class);
            then(verificationService).should().save(captor.capture());
            then(verificationSender).should().send(
                    adminEmail,
                    captor.getValue().getCode()
            );
        }
    }
}
