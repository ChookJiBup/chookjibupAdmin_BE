package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
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

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("관리자 계정 상태를 탈퇴로 저장한다")
        void success_Withdraw_Persisted() {
            // given
            AdminAccount saved = adminAccountService.save(adminAccount());
            AdminPrincipal principal = new AdminPrincipal(
                    saved.getId(),
                    saved.getEmailValue()
            );

            // when
            adminWithdrawService.withdraw(principal);

            // then
            AdminAccount found = adminAccountService.getById(saved.getId());
            assertThat(found.getStatus()).isEqualTo(AdminStatus.DELETED);
        }

        @Test
        @DisplayName("저장된 탈퇴 계정은 다시 탈퇴 처리할 수 없다")
        void fail_Withdraw_AlreadyWithdrawn_CustomException() {
            // given
            AdminAccount saved = adminAccountService.save(adminAccount());
            AdminPrincipal principal = new AdminPrincipal(
                    saved.getId(),
                    saved.getEmailValue()
            );
            adminWithdrawService.withdraw(principal);

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
