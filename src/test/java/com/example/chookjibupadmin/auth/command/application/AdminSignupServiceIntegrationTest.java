package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminContractorSignupRequest;
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
            assertThat(response.accountKind()).isEqualTo(AccountKind.GOVERNMENT);
            assertThat(response.rank()).isEqualTo("주무관");
            assertThat(saved.getAccountKind()).isEqualTo(AccountKind.GOVERNMENT);
            assertThat(saved.getOrganizationValue()).isEqualTo("관광정책과");
            assertThat(saved.getRankValue()).isEqualTo("주무관");
        }

        @Test
        @DisplayName("외부업자 계정을 DB에 저장한다")
        void success_SignupContractor_Persisted() {
            // given
            AdminContractorSignupRequest request = contractorSignupRequest();

            // when
            AdminSignupResponse response = adminSignupService.signupContractor(request);

            // then
            AdminAccount saved = adminAccountService.getByEmailForLogin(
                    AdminEmail.of(request.email())
            );
            assertThat(response.adminId()).isEqualTo(saved.getPublicId());
            assertThat(response.accountKind()).isEqualTo(AccountKind.CONTRACTOR);
            assertThat(response.rank()).isNull();
            assertThat(saved.getAccountKind()).isEqualTo(AccountKind.CONTRACTOR);
            assertThat(saved.canCreateFestival()).isFalse();
            assertThat(saved.getOrganizationValue()).isEqualTo("축제기획(주)");
        }
    }

    private AdminSignupRequest signupRequest() {
        return new AdminSignupRequest(
                "admin@mapo.go.kr",
                "홍길동",
                "관광정책과",
                "주무관",
                "Password!123",
                "Password!123"
        );
    }

    private AdminContractorSignupRequest contractorSignupRequest() {
        return new AdminContractorSignupRequest(
                "vendor@gmail.com",
                "김업체",
                "축제기획(주)",
                "Password!123",
                "Password!123"
        );
    }
}
