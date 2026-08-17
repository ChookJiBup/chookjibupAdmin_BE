package com.example.chookjibupadmin.admin.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminAccountTest {

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("관리자 계정을 탈퇴 상태로 변경한다")
        void success_Withdraw() {
            // given
            AdminAccount adminAccount = adminAccount();

            // when
            adminAccount.withdraw();

            // then
            assertThat(adminAccount.getStatus()).isEqualTo(AdminStatus.DELETED);
            assertThat(adminAccount.isDeleted()).isTrue();
            assertThat(adminAccount.isActive()).isFalse();
        }

        @Test
        @DisplayName("이미 탈퇴한 관리자 계정은 다시 탈퇴 처리할 수 없다")
        void fail_Withdraw_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();

            // when & then
            assertThatThrownBy(adminAccount::withdraw)
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_WITHDRAWN.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    @Nested
    @DisplayName("createAdmin")
    class CreateAdmin {

        @Test
        @DisplayName("관리자 계정은 외부 노출용 UUID를 가진다")
        void success_CreateAdmin() {
            // given

            // when
            AdminAccount adminAccount = adminAccount();

            // then
            assertThat(adminAccount.getPublicId()).isNotNull();
            assertThat(adminAccount.getOrganizationValue()).isEqualTo("관광정책과");
            assertThat(adminAccount.getRankValue()).isEqualTo("주무관");
        }

        @Test
        @DisplayName("관리자 계정은 활성 상태로 생성된다")
        void success_CreateAdmin_ActiveStatus() {
            // given

            // when
            AdminAccount adminAccount = adminAccount();

            // then
            assertThat(adminAccount.getStatus()).isEqualTo(AdminStatus.ACTIVE);
            assertThat(adminAccount.isActive()).isTrue();
        }

        @Test
        @DisplayName("직급이 null이면 관리자 계정을 생성할 수 없다")
        void fail_CreateAdmin_NullRank_CustomException() {
            // given
            AdminRank rank = null;

            // when & then
            assertThatThrownBy(() -> AdminAccount.createAdmin(
                    AdminEmail.of("owner@mapo.go.kr"),
                    AdminName.of("홍길동"),
                    AdminOrganization.of("관광정책과"),
                    rank,
                    AdminPasswordHash.of("encoded-password")
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
