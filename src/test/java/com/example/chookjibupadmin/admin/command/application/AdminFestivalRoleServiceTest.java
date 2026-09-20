package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminFestivalRoleServiceTest {

    @InjectMocks
    private AdminFestivalRoleService service;

    @Mock
    private AdminFestivalRoleRepository repository;

    @Nested
    @DisplayName("assignSubAdmin")
    class AssignSubAdmin {

        @Test
        @DisplayName("이미 같은 제1관리자가 배정한 대상이면 저장 없이 기존 역할을 그대로 돌려준다")
        void success_AssignSubAdmin_Idempotent() {
            // given
            AdminFestivalRole existing = AdminFestivalRole.createSubAdmin(2L, 1L, 1L);
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.of(existing));

            // when
            AdminFestivalRole result = service.assignSubAdmin(2L, 1L, 1L);

            // then
            assertThat(result).isSameAs(existing);
            then(repository).should(never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("배정된 적이 없으면 새 제2관리자 역할을 저장한다")
        void success_AssignSubAdmin_Save() {
            // given
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.empty());
            given(repository.save(org.mockito.ArgumentMatchers.any(AdminFestivalRole.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AdminFestivalRole result = service.assignSubAdmin(2L, 1L, 1L);

            // then
            assertThat(result.getRole()).isEqualTo(AdminRole.SUB_ADMIN);
            assertThat(result.getInvitedByAdminId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("다른 제1관리자가 이미 배정한 대상이면 충돌로 거절한다")
        void fail_AssignSubAdmin_OtherInviter_CustomException() {
            // given
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.of(
                            AdminFestivalRole.createSubAdmin(2L, 1L, 99L)
                    ));

            // when & then
            assertThatThrownBy(() -> service.assignSubAdmin(2L, 1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED.getMessage());
        }

        @Test
        @DisplayName("이미 총괄 역할을 가진 대상이면 충돌로 거절한다")
        void fail_AssignSubAdmin_FestivalOwner_CustomException() {
            // given
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.of(
                            AdminFestivalRole.createFestivalOwner(2L, 1L)
                    ));

            // when & then
            assertThatThrownBy(() -> service.assignSubAdmin(2L, 1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED.getMessage());
        }
    }

    @Nested
    @DisplayName("getAssignedSubAdmin")
    class GetAssignedSubAdmin {

        @Test
        @DisplayName("제약 위반 뒤 이미 저장된 배정을 다시 읽어 돌려준다")
        void success_GetAssignedSubAdmin() {
            // given
            AdminFestivalRole existing = AdminFestivalRole.createSubAdmin(2L, 1L, 1L);
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.of(existing));

            // when & then
            assertThat(service.getAssignedSubAdmin(2L, 1L, 1L)).isSameAs(existing);
        }

        @Test
        @DisplayName("저장된 배정이 없으면 충돌로 거절한다")
        void fail_GetAssignedSubAdmin_CustomException() {
            // given
            given(repository.findByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getAssignedSubAdmin(2L, 1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllByAdminAccountIdsAndFestivalId")
    class GetAllByAdminAccountIdsAndFestivalId {

        @Test
        @DisplayName("관리자 ID 목록과 축제 ID에 해당하는 역할을 조회한다")
        void success_GetAllByAdminAccountIdsAndFestivalId() {
            // given
            List<Long> accountIds = List.of(2L, 3L);
            List<AdminFestivalRole> roles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L),
                    AdminFestivalRole.createSubAdmin(3L, 1L, 1L)
            );
            given(repository.findAllByAdminAccountIdInAndFestivalId(
                    accountIds,
                    1L
            )).willReturn(roles);

            // when
            List<AdminFestivalRole> found =
                    service.getAllByAdminAccountIdsAndFestivalId(accountIds, 1L);

            // then
            assertThat(found).containsExactlyElementsOf(roles);
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("관리자 축제 역할 삭제를 저장소에 위임한다")
        void success_DeleteAll() {
            // given
            List<AdminFestivalRole> roles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L)
            );

            // when
            service.deleteAll(roles);

            // then
            then(repository).should().deleteAll(roles);
        }
    }

    @Nested
    @DisplayName("hasFestivalOwnerRole")
    class HasFestivalOwnerRole {

        @Test
        @DisplayName("총괄 역할이 있으면 true를 반환한다")
        void success_HasFestivalOwnerRole() {
            // given
            given(repository.existsByAdminAccountIdAndRole(1L, AdminRole.FESTIVAL_OWNER))
                    .willReturn(true);

            // when
            boolean exists = service.hasFestivalOwnerRole(1L);

            // then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("getHighestRole")
    class GetHighestRole {

        @Test
        @DisplayName("총괄과 운영자를 함께 가지면 총괄을 대표 역할로 고른다")
        void success_GetHighestRole_OwnerAndSubAdmin() {
            // given: 총괄 10곳 + 운영자 3곳이어도 역할 종류는 두 줄만 돌아온다.
            given(repository.findDistinctRolesByAdminAccountId(1L))
                    .willReturn(List.of(AdminRole.SUB_ADMIN, AdminRole.FESTIVAL_OWNER));

            // when
            AdminRole role = service.getHighestRole(1L);

            // then
            assertThat(role).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }

        @Test
        @DisplayName("운영자 역할만 가지면 운영자를 대표 역할로 고른다")
        void success_GetHighestRole_SubAdminOnly() {
            // given
            given(repository.findDistinctRolesByAdminAccountId(2L))
                    .willReturn(List.of(AdminRole.SUB_ADMIN));

            // when
            AdminRole role = service.getHighestRole(2L);

            // then
            assertThat(role).isEqualTo(AdminRole.SUB_ADMIN);
        }

        @Test
        @DisplayName("배정된 축제가 하나도 없으면 대표 역할이 없다")
        void success_GetHighestRole_NoAssignment() {
            // given
            given(repository.findDistinctRolesByAdminAccountId(11L))
                    .willReturn(List.of());

            // when
            AdminRole role = service.getHighestRole(11L);

            // then
            assertThat(role).isNull();
        }

        @Test
        @DisplayName("아직 저장되지 않은 계정이면 역할을 조회하지 않는다")
        void success_GetHighestRole_NullAccountId() {
            // when
            AdminRole role = service.getHighestRole(null);

            // then
            assertThat(role).isNull();
            then(repository).should(never())
                    .findDistinctRolesByAdminAccountId(org.mockito.ArgumentMatchers.any());
        }
    }
}
