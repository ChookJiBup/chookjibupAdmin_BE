package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginResponse;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminLoginServiceIntegrationTest {

    @Autowired
    private AdminLoginService adminLoginService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("저장된 관리자 계정으로 로그인하고 JWT를 발급한다")
        void success_Login_PersistedAdmin() {
            // given
            AdminAccount adminAccount = adminAccount("Password!123");
            adminAccountService.save(adminAccount);
            AdminLoginRequest request = new AdminLoginRequest(
                    "admin@mapo.go.kr",
                    "Password!123"
            );

            // when
            AdminLoginResponse response = adminLoginService.login(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.admin().email()).isEqualTo(request.email());
            assertThat(response.admin().accountKind()).isEqualTo(AccountKind.GOVERNMENT);
            assertThat(response.admin().festivalId()).isNull();
            assertThat(response.admin().role()).isNull();
        }

        @Test
        @DisplayName("외부업자 일반 이메일로 로그인한다")
        void success_Login_ContractorEmail() {
            // given
            AdminAccount adminAccount = AdminAccount.createContractor(
                    AdminEmail.of("vendor@gmail.com"),
                    AdminName.of("김업체"),
                    AdminOrganization.of("축제기획(주)"),
                    AdminPasswordHash.of(passwordEncoder.encode("Password!123"))
            );
            adminAccountService.save(adminAccount);
            AdminLoginRequest request = new AdminLoginRequest(
                    "vendor@gmail.com",
                    "Password!123"
            );

            // when
            AdminLoginResponse response = adminLoginService.login(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.admin().email()).isEqualTo("vendor@gmail.com");
            assertThat(response.admin().accountKind()).isEqualTo(AccountKind.CONTRACTOR);
            assertThat(response.admin().rank()).isNull();
        }

        @Test
        @DisplayName("총괄과 운영자를 함께 맡은 계정은 총괄을 대표 역할로 내려준다")
        void success_Login_HighestRoleAcrossFestivals() {
            // given: 총괄 2곳 + 운영자 1곳. 배정 수와 무관하게 역할 종류만 조회한다.
            AdminAccount adminAccount = adminAccount("Password!123");
            adminAccountService.save(adminAccount);
            Long adminId = adminAccount.getId();
            adminFestivalRoleService.save(
                    AdminFestivalRole.createFestivalOwner(adminId, 101L));
            adminFestivalRoleService.save(
                    AdminFestivalRole.createFestivalOwner(adminId, 102L));
            adminFestivalRoleService.save(
                    AdminFestivalRole.createSubAdmin(adminId, 103L, 999L));

            // when
            AdminLoginResponse response = adminLoginService.login(new AdminLoginRequest(
                    "admin@mapo.go.kr",
                    "Password!123"
            ));

            // then
            assertThat(response.admin().role()).isEqualTo(AdminRole.FESTIVAL_OWNER);
            // 권한 플래그는 축제별 값이라 계정 단위로는 채우지 않는다.
            assertThat(response.admin().canInviteSubAdmin()).isFalse();
        }

        @Test
        @DisplayName("운영자로만 배정된 계정은 운영자를 대표 역할로 내려준다")
        void success_Login_SubAdminOnly() {
            // given
            AdminAccount adminAccount = adminAccount("Password!123");
            adminAccountService.save(adminAccount);
            Long adminId = adminAccount.getId();
            adminFestivalRoleService.save(
                    AdminFestivalRole.createSubAdmin(adminId, 201L, 999L));
            adminFestivalRoleService.save(
                    AdminFestivalRole.createSubAdmin(adminId, 202L, 999L));

            // when
            AdminLoginResponse response = adminLoginService.login(new AdminLoginRequest(
                    "admin@mapo.go.kr",
                    "Password!123"
            ));

            // then
            assertThat(response.admin().role()).isEqualTo(AdminRole.SUB_ADMIN);
        }

        @Test
        @DisplayName("탈퇴 상태로 저장된 관리자 계정은 로그인할 수 없다")
        void fail_Login_WithdrawnAdmin_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount("Password!123");
            adminAccount.withdraw();
            adminAccountService.save(adminAccount);
            AdminLoginRequest request = new AdminLoginRequest(
                    "admin@mapo.go.kr",
                    "Password!123"
            );

            // when & then
            assertThatThrownBy(() -> adminLoginService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
        }
    }

    private AdminAccount adminAccount(String rawPassword) {
        return AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of(passwordEncoder.encode(rawPassword))
        );
    }
}
