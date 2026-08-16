package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffCommand;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginCommand;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginResult;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FieldStaffLoginServiceIntegrationTest {

    @Autowired
    private FieldStaffManagementService managementService;

    @Autowired
    private FieldStaffLoginService loginService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("생성된 임시 비밀번호로 현장 스태프 로그인을 처리한다")
        void success_Login_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = persistOwner(festival);
            CreateFieldStaffResult created = managementService.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal(adminAccount)
            );

            // when
            FieldStaffLoginResult result = loginService.login(new FieldStaffLoginCommand(
                    festival.getPublicId(),
                    "staff01",
                    created.temporaryPassword()
            ));

            // then
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.expiresIn()).isEqualTo(1800L);
            assertThat(result.fieldStaffAccount().getLoginIdValue()).isEqualTo("staff01");
        }
    }

    private CreateFieldStaffCommand createCommand() {
        return new CreateFieldStaffCommand(
                "staff01",
                "김스태프",
                "010-1234-5678"
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private AdminAccount persistOwner(Festival festival) {
        AdminAccount owner = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
        return owner;
    }

    private Festival festival() {
        LocalDate today = LocalDate.now();
        return Festival.create(
                10L,
                java.util.UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(today.minusDays(1), today.plusDays(1)),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
