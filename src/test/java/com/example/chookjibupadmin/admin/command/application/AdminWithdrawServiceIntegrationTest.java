package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminWithdrawServiceIntegrationTest {

    @Autowired
    private AdminWithdrawService adminWithdrawService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("관리자 계정 상태를 탈퇴로 저장한다")
        void success_Withdraw_Persisted() {
            // given
            AdminAccount saved = adminAccountService.save(governmentAccount("admin@mapo.go.kr"));
            AdminPrincipal principal = principal(saved);

            // when
            adminWithdrawService.withdraw(principal);

            // then
            AdminAccount found = adminAccountService.getById(saved.getId());
            assertThat(found.getStatus()).isEqualTo(AdminStatus.DELETED);
        }

        @Test
        @DisplayName("운영자(제2관리자)는 탈퇴할 수 있다")
        void success_Withdraw_SubAdmin() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = adminAccountService.save(governmentAccount("owner@mapo.go.kr"));
            AdminAccount operator = adminAccountService.save(contractorAccount("vendor@gmail.com"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
            adminFestivalRoleService.assignSubAdmin(operator.getId(), festival.getId(), owner.getId());

            // when
            adminWithdrawService.withdraw(principal(operator));

            // then
            assertThat(adminAccountService.getById(operator.getId()).getStatus())
                    .isEqualTo(AdminStatus.DELETED);
            assertThat(adminAccountService.getById(owner.getId()).getStatus())
                    .isEqualTo(AdminStatus.ACTIVE);
        }

        @Test
        @DisplayName("저장된 탈퇴 계정은 다시 탈퇴 처리할 수 없다")
        void fail_Withdraw_AlreadyWithdrawn_CustomException() {
            // given
            AdminAccount saved = adminAccountService.save(governmentAccount("admin@mapo.go.kr"));
            AdminPrincipal principal = principal(saved);
            adminWithdrawService.withdraw(principal);

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("총괄 관리자 역할이 있으면 탈퇴할 수 없다")
        void fail_Withdraw_HasOwnerRole_CustomException() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = adminAccountService.save(governmentAccount("owner@mapo.go.kr"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal(owner)))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_WITHDRAW_HAS_OWNER_ROLE.getMessage());
            assertThat(adminAccountService.getById(owner.getId()).getStatus())
                    .isEqualTo(AdminStatus.ACTIVE);
        }
    }

    private AdminPrincipal principal(AdminAccount account) {
        return new AdminPrincipal(account.getId(), account.getEmailValue());
    }

    private AdminAccount governmentAccount(String email) {
        return AdminAccount.createGovernment(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount contractorAccount(String email) {
        return AdminAccount.createContractor(
                AdminEmail.of(email),
                AdminName.of("김업체"),
                AdminOrganization.of("축제기획(주)"),
                AdminPasswordHash.of("encoded-password")
        );
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
