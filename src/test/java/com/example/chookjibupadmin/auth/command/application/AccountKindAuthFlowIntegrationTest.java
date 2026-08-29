package com.example.chookjibupadmin.auth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminContractorSignupRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginResponse;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupRequest;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountKindAuthFlowIntegrationTest {

    @Autowired
    private AdminSignupService adminSignupService;

    @Autowired
    private AdminLoginService adminLoginService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private FestivalApplicationService festivalApplicationService;

    @MockitoBean
    private AdminEmailVerificationService emailVerificationService;

    @Test
    @DisplayName("공무원 가입 후 로그인하면 축제를 생성할 수 있다")
    void success_GovernmentSignupLoginAndCreateFestival() {
        // given
        adminSignupService.signupGovernment(new AdminSignupRequest(
                "owner@mapo.go.kr",
                "홍길동",
                "관광정책과",
                "주무관",
                "Password!123",
                "Password!123"
        ));

        // when
        AdminLoginResponse login = adminLoginService.login(new AdminLoginRequest(
                "owner@mapo.go.kr",
                "Password!123"
        ));
        AdminAccount account = adminAccountService.getByEmailForLogin(
                AdminEmail.of("owner@mapo.go.kr")
        );
        Festival festival = festivalApplicationService.create(
                createCommand(),
                new AdminPrincipal(account.getId(), account.getEmailValue())
        );

        // then
        assertThat(login.admin().accountKind()).isEqualTo(AccountKind.GOVERNMENT);
        assertThat(festival.getId()).isNotNull();
        assertThat(login.admin().role()).isNull();
        assertThat(account.canCreateFestival()).isTrue();
    }

    @Test
    @DisplayName("외부업자 가입 후 로그인해도 축제를 생성할 수 없다")
    void fail_ContractorSignupLoginAndCreateFestival() {
        // given
        adminSignupService.signupContractor(new AdminContractorSignupRequest(
                "vendor@gmail.com",
                "김업체",
                "축제기획(주)",
                "Password!123",
                "Password!123"
        ));

        // when
        AdminLoginResponse login = adminLoginService.login(new AdminLoginRequest(
                "vendor@gmail.com",
                "Password!123"
        ));
        AdminAccount account = adminAccountService.getByEmailForLogin(
                AdminEmail.of("vendor@gmail.com")
        );

        // then
        assertThat(login.admin().accountKind()).isEqualTo(AccountKind.CONTRACTOR);
        assertThatThrownBy(() -> festivalApplicationService.create(
                createCommand(),
                new AdminPrincipal(account.getId(), account.getEmailValue())
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.AUTH_FESTIVAL_CREATE_FORBIDDEN.getMessage());
    }

    private CreateFestivalCommand createCommand() {
        return new CreateFestivalCommand(
                null,
                "마포나루 새우젓축제",
                "마포구 대표 지역 축제",
                List.of(
                        new FestivalLocationCommand(
                                FestivalLocationType.MAIN_VENUE,
                                "월드컵공원",
                                "서울특별시 마포구 월드컵로 243",
                                null,
                                "중앙광장",
                                null,
                                null,
                                new java.math.BigDecimal("37.5683000"),
                                new java.math.BigDecimal("126.8973000"),
                                true,
                                0
                        )
                ),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }
}
