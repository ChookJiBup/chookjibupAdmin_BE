package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupRequest;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSignupServiceTest {

    @InjectMocks
    private AdminSignupService adminSignupService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminEmailVerificationService emailVerificationService;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("이메일 인증이 완료되면 관리자 계정으로 가입한다")
        void success_Signup_VerifiedEmail() {
            // given
            AdminSignupRequest request = signupRequest("admin@mapo.go.kr");
            given(adminAccountService.existsByEmail(AdminEmail.of(request.email())))
                    .willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encoded-password");
            given(adminAccountService.save(any(AdminAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            var response = adminSignupService.signup(request);

            // then
            assertThat(response.email()).isEqualTo("admin@mapo.go.kr");
            assertThat(response.department()).isEqualTo("관광정책과");
            assertThat(response.rank()).isEqualTo("주무관");
            assertThat(response.festivalId()).isNull();
            assertThat(response.role()).isNull();
            then(emailVerificationService)
                    .should()
                    .ensureVerified(AdminEmail.of(request.email()));
            then(emailVerificationService)
                    .should()
                    .consumeVerified(AdminEmail.of(request.email()));
            then(adminAccountService).should().save(any(AdminAccount.class));
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않으면 가입할 수 없다")
        void fail_Signup_EmailNotVerified_CustomException() {
            // given
            AdminSignupRequest request = signupRequest("admin@mapo.go.kr");
            given(adminAccountService.existsByEmail(AdminEmail.of(request.email())))
                    .willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encoded-password");
            doThrow(new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED))
                    .when(emailVerificationService)
                    .ensureVerified(AdminEmail.of(request.email()));

            // when & then
            assertThatThrownBy(() -> adminSignupService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_NOT_VERIFIED.getMessage());
        }
    }

    private AdminSignupRequest signupRequest(String email) {
        return new AdminSignupRequest(
                email,
                "홍길동",
                "마포구청 소속",
                "관광정책과",
                "주무관",
                "Password!123",
                "Password!123"
        );
    }
}
