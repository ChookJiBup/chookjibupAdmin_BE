package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.dto.RegisterOperatorResult;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
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
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminOperatorRegistrationServiceIntegrationTest {

    @Autowired
    private AdminOperatorRegistrationService operatorRegistrationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Test
    @DisplayName("미가입 외부 이메일이면 외부업자 계정을 만들고 운영자로 배정한다")
    void success_Register_CreateContractor() {
        // given
        Festival festival = persistFestivalWithOwner();
        AdminAccount owner = owner();

        // when
        RegisterOperatorResult result = operatorRegistrationService.register(
                festival.getPublicId(),
                "new-vendor@gmail.com",
                "김운영",
                "축제기획(주)",
                principal(owner)
        );

        // then
        AdminAccount created = adminAccountService.getByEmailForLogin(
                AdminEmail.of("new-vendor@gmail.com")
        );
        AdminFestivalRole role = adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                created.getId(),
                festival.getId()
        );
        assertThat(result.created()).isTrue();
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(created.getAccountKind()).isEqualTo(AccountKind.CONTRACTOR);
        assertThat(role.getRole()).isEqualTo(AdminRole.SUB_ADMIN);
    }

    @Test
    @DisplayName("기존 외부업자 계정이면 운영자 역할만 배정한다")
    void success_Register_AssignExistingContractor() {
        // given
        Festival festival = persistFestivalWithOwner();
        AdminAccount owner = owner();
        AdminAccount contractor = adminAccountService.save(AdminAccount.createContractor(
                AdminEmail.of("existing-vendor@gmail.com"),
                AdminName.of("기존업체"),
                AdminOrganization.of("기존기획(주)"),
                AdminPasswordHash.of("encoded-password")
        ));

        // when
        RegisterOperatorResult result = operatorRegistrationService.register(
                festival.getPublicId(),
                "existing-vendor@gmail.com",
                "무시될이름",
                "무시될업체",
                principal(owner)
        );

        // then
        AdminFestivalRole role = adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                contractor.getId(),
                festival.getId()
        );
        assertThat(result.created()).isFalse();
        assertThat(result.temporaryPassword()).isNull();
        assertThat(result.adminId()).isEqualTo(contractor.getPublicId());
        assertThat(role.getRole()).isEqualTo(AdminRole.SUB_ADMIN);
    }

    @Test
    @DisplayName("운영자는 운영자를 등록할 수 없다")
    void fail_Register_SubAdminForbidden() {
        // given
        Festival festival = persistFestivalWithOwner();
        AdminAccount owner = owner();
        operatorRegistrationService.register(
                festival.getPublicId(),
                "operator@gmail.com",
                "김운영",
                "축제기획(주)",
                principal(owner)
        );
        AdminAccount operator = adminAccountService.getByEmailForLogin(
                AdminEmail.of("operator@gmail.com")
        );

        // when & then
        assertThatThrownBy(() -> operatorRegistrationService.register(
                festival.getPublicId(),
                "another@gmail.com",
                "다른업체",
                "다른기획(주)",
                principal(operator)
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    private Festival persistFestivalWithOwner() {
        Festival festival = festivalService.save(festival());
        AdminAccount owner = adminAccountService.save(AdminAccount.createGovernment(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("총괄"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("과장"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
        return festival;
    }

    private AdminAccount owner() {
        return adminAccountService.getByEmailForLogin(AdminEmail.of("owner@mapo.go.kr"));
    }

    private AdminPrincipal principal(AdminAccount account) {
        return new AdminPrincipal(account.getId(), account.getEmailValue());
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0))
        );
    }
}
