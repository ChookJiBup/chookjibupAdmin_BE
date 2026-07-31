package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminSignupServiceIntegrationTest {

    @Autowired
    private AdminSignupService adminSignupService;

    @Autowired
    private AdminAccountService adminAccountService;

    @MockitoBean
    private AdminEmailVerificationService emailVerificationService;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("관리자 계정을 DB에 저장한다")
        void success_Signup_Persisted() {
            // given
            AdminSignupRequest request = signupRequest();

            // when
            AdminSignupResponse response = adminSignupService.signup(request);

            // then
            AdminAccount saved = adminAccountService.getByEmailForLogin(
                    AdminEmail.of(request.email())
            );
            assertThat(response.adminId()).isEqualTo(saved.getPublicId());
            assertThat(saved.getFestivalId()).isNull();
            assertThat(saved.getRole()).isNull();
        }
    }

    private AdminSignupRequest signupRequest() {
        return new AdminSignupRequest(
                "admin@mapo.go.kr",
                "홍길동",
                "마포구청 소속",
                "Password!123",
                "Password!123"
        );
    }
}
