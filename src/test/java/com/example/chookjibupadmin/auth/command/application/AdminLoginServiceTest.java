package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    @InjectMocks
    private AdminLoginService adminLoginService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

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
        @DisplayName("총괄과 운영자를 함께 맡은 계정은 총괄을 대표 역할로 내려준다")
        void success_Login_HighestRoleFestivalOwner() {
            // given
            AdminLoginResponse response = loginWithHighestRole(AdminRole.FESTIVAL_OWNER);

            // then
            assertThat(response.admin().role()).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }

        @Test
        @DisplayName("운영자만 맡은 계정은 운영자를 대표 역할로 내려준다")
        void success_Login_HighestRoleSubAdmin() {
            // given
            AdminLoginResponse response = loginWithHighestRole(AdminRole.SUB_ADMIN);

            // then
            assertThat(response.admin().role()).isEqualTo(AdminRole.SUB_ADMIN);
        }

        @Test
        @DisplayName("배정된 축제가 없으면 대표 역할 없이 응답한다")
        void success_Login_NoAssignedFestival() {
            // given
            AdminLoginResponse response = loginWithHighestRole(null);

            // then
            assertThat(response.admin().role()).isNull();
        }

        @Test
        @DisplayName("대표 역할이 총괄이어도 권한 플래그는 축제별 값이라 채우지 않는다")
        void success_Login_KeepsPermissionFlagsFalse() {
            // given
            AdminLoginResponse response = loginWithHighestRole(AdminRole.FESTIVAL_OWNER);

            // then: 계정 단위로 true를 내려주면 운영자로 배정된 축제에서도 총괄 메뉴가 열린다.
            assertThat(response.admin().festivalId()).isNull();
            assertThat(response.admin().canInviteSubAdmin()).isFalse();
            assertThat(response.admin().canModifyFestivalInfo()).isFalse();
            assertThat(response.admin().canViewOperationReport()).isFalse();
            assertThat(response.admin().canUpdateQueueTail()).isFalse();
        }

        private AdminLoginResponse loginWithHighestRole(AdminRole highestRole) {
            AdminLoginRequest request = loginRequest();
            AdminAccount adminAccount = adminAccount();
            ReflectionTestUtils.setField(adminAccount, "id", 7L);
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
            given(adminFestivalRoleService.getHighestRole(7L)).willReturn(highestRole);

            return adminLoginService.login(request);
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
