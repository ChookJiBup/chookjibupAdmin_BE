package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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
            assertThat(response.admin().festivalId()).isNull();
            assertThat(response.admin().role()).isNull();
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
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of(passwordEncoder.encode(rawPassword))
        );
    }
}
