package com.example.demoadmin.admin.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminFestivalRoleTest {

    @Nested
    @DisplayName("createFestivalOwner")
    class CreateFestivalOwner {

        @Test
        @DisplayName("축제를 생성한 관리자를 1관리자로 배정한다")
        void success_CreateFestivalOwner() {
            // given
            Long adminAccountId = 1L;
            Long festivalId = 1L;

            // when
            AdminFestivalRole role = AdminFestivalRole.createFestivalOwner(
                    adminAccountId,
                    festivalId
            );

            // then
            assertThat(role.getAdminAccountId()).isEqualTo(adminAccountId);
            assertThat(role.getFestivalId()).isEqualTo(festivalId);
            assertThat(role.getRole()).isEqualTo(AdminRole.FESTIVAL_OWNER);
            assertThat(role.getInvitedByAdminId()).isNull();
            assertThat(role.canInviteSubAdmin()).isTrue();
            assertThat(role.canModifyFestivalInfo()).isTrue();
            assertThat(role.canManageFieldStaff()).isTrue();
            assertThat(role.canViewOperationReport()).isTrue();
        }

        @Test
        @DisplayName("계정 ID가 null이면 1관리자로 배정할 수 없다")
        void fail_CreateFestivalOwner_NullAdminAccountId_CustomException() {
            // given
            Long adminAccountId = null;
            Long festivalId = 1L;

            // when & then
            assertThatThrownBy(() -> AdminFestivalRole.createFestivalOwner(
                    adminAccountId,
                    festivalId
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 ID가 null이면 1관리자로 배정할 수 없다")
        void fail_CreateFestivalOwner_NullFestivalId_CustomException() {
            // given
            Long adminAccountId = 1L;
            Long festivalId = null;

            // when & then
            assertThatThrownBy(() -> AdminFestivalRole.createFestivalOwner(
                    adminAccountId,
                    festivalId
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("createSubAdmin")
    class CreateSubAdmin {

        @Test
        @DisplayName("1관리자가 초대한 관리자를 2관리자로 배정한다")
        void success_CreateSubAdmin() {
            // given
            Long adminAccountId = 2L;
            Long festivalId = 1L;
            Long invitedByAdminId = 1L;

            // when
            AdminFestivalRole role = AdminFestivalRole.createSubAdmin(
                    adminAccountId,
                    festivalId,
                    invitedByAdminId
            );

            // then
            assertThat(role.getAdminAccountId()).isEqualTo(adminAccountId);
            assertThat(role.getFestivalId()).isEqualTo(festivalId);
            assertThat(role.getRole()).isEqualTo(AdminRole.SUB_ADMIN);
            assertThat(role.getInvitedByAdminId()).isEqualTo(invitedByAdminId);
            assertThat(role.canInviteSubAdmin()).isFalse();
            assertThat(role.canModifyFestivalInfo()).isFalse();
            assertThat(role.canManageFieldStaff()).isTrue();
            assertThat(role.canViewOperationReport()).isTrue();
        }

        @Test
        @DisplayName("초대한 관리자 ID가 null이면 2관리자로 배정할 수 없다")
        void fail_CreateSubAdmin_NullInvitedByAdminId_CustomException() {
            // given
            Long invitedByAdminId = null;

            // when & then
            assertThatThrownBy(() -> AdminFestivalRole.createSubAdmin(
                    2L,
                    1L,
                    invitedByAdminId
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
