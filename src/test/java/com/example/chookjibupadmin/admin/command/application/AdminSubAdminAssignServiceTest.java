package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSubAdminAssignServiceTest {

    @Mock
    private AdminAccountService accountService;

    @Mock
    private AdminFestivalRoleService roleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminAccount owner;

    @Mock
    private AdminAccount target;

    @Mock
    private Festival festival;

    @Mock
    private AdminFestivalRole ownerRole;

    @Mock
    private AdminFestivalRole assignedRole;

    @Test
    void success_Assign() {
        UUID festivalId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        given(accountService.getById(1L)).willReturn(owner);
        given(owner.isActive()).willReturn(true);
        given(owner.getId()).willReturn(1L);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(festival.getId()).willReturn(10L);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L))
                .willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(true);
        given(accountService.findByPublicId(targetId)).willReturn(Optional.of(target));
        given(target.isActive()).willReturn(true);
        given(target.getId()).willReturn(2L);
        given(roleService.assignSubAdmin(2L, 10L, 1L)).willReturn(assignedRole);
        AdminSubAdminAssignService service = new AdminSubAdminAssignService(
                accountService,
                roleService,
                festivalService
        );

        AdminFestivalRole result = service.assign(
                festivalId,
                targetId,
                new AdminPrincipal(1L, "owner@mapo.go.kr")
        );

        assertThat(result).isSameAs(assignedRole);
    }

    @Test
    void fail_Assign_TargetNotFound_CustomException() {
        UUID festivalId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        given(accountService.getById(1L)).willReturn(owner);
        given(owner.isActive()).willReturn(true);
        given(owner.getId()).willReturn(1L);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(festival.getId()).willReturn(10L);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L))
                .willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(true);
        given(accountService.findByPublicId(targetId)).willReturn(Optional.empty());
        AdminSubAdminAssignService service = new AdminSubAdminAssignService(
                accountService,
                roleService,
                festivalService
        );

        assertThatThrownBy(() -> service.assign(
                festivalId,
                targetId,
                new AdminPrincipal(1L, "owner@mapo.go.kr")
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND.getMessage());
    }

    @Test
    void fail_Assign_SubAdminCannotInvite_CustomException() {
        UUID festivalId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        given(accountService.getById(1L)).willReturn(owner);
        given(owner.isActive()).willReturn(true);
        given(owner.getId()).willReturn(1L);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(festival.getId()).willReturn(10L);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L))
                .willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(false);
        AdminSubAdminAssignService service = new AdminSubAdminAssignService(
                accountService,
                roleService,
                festivalService
        );

        assertThatThrownBy(() -> service.assign(
                festivalId,
                targetId,
                new AdminPrincipal(1L, "sub@mapo.go.kr")
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }
}
