package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminWithdrawServiceTest {

    @InjectMocks
    private AdminWithdrawService adminWithdrawService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("인증된 관리자 계정을 탈퇴 상태로 변경한다")
        void success_Withdraw() {
            // given
            AdminAccount adminAccount = adminAccount();
            ReflectionTestUtils.setField(adminAccount, "id", 1L);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(adminFestivalRoleService.hasFestivalOwnerRole(1L)).willReturn(false);

            // when
            adminWithdrawService.withdraw(principal);

            // then
            assertThat(adminAccount.getStatus()).isEqualTo(AdminStatus.DELETED);
            then(adminAccountService).should().getById(1L);
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_Withdraw_CustomException() {
            // given
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("이미 탈퇴한 계정이면 탈퇴 예외를 던진다")
        void fail_Withdraw_AlreadyWithdrawn_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            given(adminAccountService.getById(1L)).willReturn(adminAccount);

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN.getMessage());
            then(adminFestivalRoleService).should(never()).hasFestivalOwnerRole(org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("총괄 관리자 역할이 남아 있으면 탈퇴할 수 없다")
        void fail_Withdraw_HasOwnerRole_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            ReflectionTestUtils.setField(adminAccount, "id", 1L);
            AdminPrincipal principal = new AdminPrincipal(1L, "admin@mapo.go.kr");
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(adminFestivalRoleService.hasFestivalOwnerRole(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> adminWithdrawService.withdraw(principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_WITHDRAW_HAS_OWNER_ROLE.getMessage());
            assertThat(adminAccount.getStatus()).isEqualTo(AdminStatus.ACTIVE);
        }
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createGovernment(
                AdminEmail.of("admin@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
