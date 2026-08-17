package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginResponse;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
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
class AdminLoginServiceTest {

    @InjectMocks
    private AdminLoginService adminLoginService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("관리자 이메일과 비밀번호가 일치하면 JWT를 발급한다")
        void success_Login() {
            // given
            AdminLoginRequest request = loginRequest();
            AdminAccount adminAccount = adminAccount();
            given(adminAccountService.getByEmailForLogin(AdminEmail.of(request.email())))
                    .willReturn(adminAccount);
            given(passwordEncoder.matches(
                    request.password(),
                    adminAccount.getPasswordHashValue()
            )).willReturn(true);
            given(jwtTokenProvider.createAccessToken(adminAccount))
                    .willReturn("access-token");
            given(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .willReturn(1800L);

            // when
            AdminLoginResponse response = adminLoginService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.expiresIn()).isEqualTo(1800L);
            assertThat(response.admin().email()).isEqualTo(request.email());
            assertThat(response.admin().organization()).isEqualTo("관광정책과");
            assertThat(response.admin().rank()).isEqualTo("주무관");
        }

        @Test
        @DisplayName("축제에 배정된 관리자도 로그인 응답에서는 축제 역할을 확정하지 않는다")
        void success_Login_WithoutFestivalRole() {
            // given
            AdminLoginRequest request = loginRequest();
            AdminAccount adminAccount = adminAccount();
            given(adminAccountService.getByEmailForLogin(AdminEmail.of(request.email())))
                    .willReturn(adminAccount);
            given(passwordEncoder.matches(
                    request.password(),
                    adminAccount.getPasswordHashValue()
            )).willReturn(true);
            given(jwtTokenProvider.createAccessToken(adminAccount))
                    .willReturn("access-token");
            given(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .willReturn(1800L);

            // when
            AdminLoginResponse response = adminLoginService.login(request);

            // then
            assertThat(response.admin().festivalId()).isNull();
            assertThat(response.admin().role()).isNull();
            assertThat(response.admin().canInviteSubAdmin()).isFalse();
        }

        @Test
        @DisplayName("이메일에 해당하는 계정이 없으면 로그인할 수 없다")
        void fail_Login_InvalidCredentials_CustomException() {
            // given
            AdminLoginRequest request = loginRequest();
            given(adminAccountService.getByEmailForLogin(AdminEmail.of(request.email())))
                    .willThrow(new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));

            // when & then
            assertThatThrownBy(() -> adminLoginService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 로그인할 수 없다")
        void fail_Login_PasswordMismatch_CustomException() {
            // given
            AdminLoginRequest request = loginRequest();
            AdminAccount adminAccount = adminAccount();
            given(adminAccountService.getByEmailForLogin(AdminEmail.of(request.email())))
                    .willReturn(adminAccount);
            given(passwordEncoder.matches(
                    request.password(),
                    adminAccount.getPasswordHashValue()
            )).willReturn(false);

            // when & then
            assertThatThrownBy(() -> adminLoginService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 관리자 계정은 로그인할 수 없다")
        void fail_Login_InactiveAdmin_CustomException() {
            // given
            AdminLoginRequest request = loginRequest();
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();
            given(adminAccountService.getByEmailForLogin(AdminEmail.of(request.email())))
                    .willReturn(adminAccount);
            given(passwordEncoder.matches(
                    request.password(),
                    adminAccount.getPasswordHashValue()
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> adminLoginService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
        }
    }

    private AdminLoginRequest loginRequest() {
        return new AdminLoginRequest(
                "admin@mapo.go.kr",
                "Password!123"
        );
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

}
