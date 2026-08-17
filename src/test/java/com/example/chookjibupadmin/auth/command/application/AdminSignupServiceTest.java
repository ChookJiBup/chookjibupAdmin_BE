package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminContractorSignupRequest;
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
    @DisplayName("signupGovernment")
    class SignupGovernment {

        @Test
        @DisplayName("이메일 인증이 완료되면 공무원 계정으로 가입한다")
        void success_SignupGovernment_VerifiedEmail() {
            // given
            AdminSignupRequest request = governmentSignupRequest("admin@mapo.go.kr");
            given(adminAccountService.existsByEmail(
                    AdminEmail.of(request.email(), AccountKind.GOVERNMENT)
            )).willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encoded-password");
            given(adminAccountService.save(any(AdminAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            var response = adminSignupService.signupGovernment(request);

            // then
            assertThat(response.email()).isEqualTo("admin@mapo.go.kr");
            assertThat(response.accountKind()).isEqualTo(AccountKind.GOVERNMENT);
            assertThat(response.rank()).isEqualTo("주무관");
            then(emailVerificationService)
                    .should()
                    .ensureVerified(
                            AdminEmail.of(request.email(), AccountKind.GOVERNMENT),
                            AccountKind.GOVERNMENT
                    );
            then(emailVerificationService)
                    .should()
                    .consumeVerified(
                            AdminEmail.of(request.email(), AccountKind.GOVERNMENT),
                            AccountKind.GOVERNMENT
                    );
        }

        @Test
        @DisplayName("일반 이메일이면 공무원 가입할 수 없다")
        void fail_SignupGovernment_GeneralEmail() {
            // given
            AdminSignupRequest request = governmentSignupRequest("vendor@gmail.com");

            // when & then
            assertThatThrownBy(() -> adminSignupService.signupGovernment(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_GOVERNMENT_EMAIL_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않으면 가입할 수 없다")
        void fail_SignupGovernment_EmailNotVerified() {
            // given
            AdminSignupRequest request = governmentSignupRequest("admin@mapo.go.kr");
            given(adminAccountService.existsByEmail(
                    AdminEmail.of(request.email(), AccountKind.GOVERNMENT)
            )).willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encoded-password");
            doThrow(new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED))
                    .when(emailVerificationService)
                    .ensureVerified(
                            AdminEmail.of(request.email(), AccountKind.GOVERNMENT),
                            AccountKind.GOVERNMENT
                    );

            // when & then
            assertThatThrownBy(() -> adminSignupService.signupGovernment(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_NOT_VERIFIED.getMessage());
        }
    }

    @Nested
    @DisplayName("signupContractor")
    class SignupContractor {

        @Test
        @DisplayName("이메일 인증이 완료되면 외부업자 계정으로 가입한다")
        void success_SignupContractor_VerifiedEmail() {
            // given
            AdminContractorSignupRequest request = contractorSignupRequest("vendor@gmail.com");
            given(adminAccountService.existsByEmail(
                    AdminEmail.of(request.email(), AccountKind.CONTRACTOR)
            )).willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encoded-password");
            given(adminAccountService.save(any(AdminAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            var response = adminSignupService.signupContractor(request);

            // then
            assertThat(response.email()).isEqualTo("vendor@gmail.com");
            assertThat(response.accountKind()).isEqualTo(AccountKind.CONTRACTOR);
            assertThat(response.rank()).isNull();
        }
    }

    private AdminSignupRequest governmentSignupRequest(String email) {
        return new AdminSignupRequest(
                email,
                "홍길동",
                "관광정책과",
                "주무관",
                "Password!123",
                "Password!123"
        );
    }

    private AdminContractorSignupRequest contractorSignupRequest(String email) {
        return new AdminContractorSignupRequest(
                email,
                "김업체",
                "축제기획(주)",
                "Password!123",
                "Password!123"
        );
    }
}
