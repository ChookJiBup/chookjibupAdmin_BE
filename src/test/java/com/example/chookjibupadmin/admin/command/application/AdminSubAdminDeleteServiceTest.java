package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminSubAdminDeleteServiceTest {

    @InjectMocks
    private AdminSubAdminDeleteService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("제1 관리자가 자신이 초대한 제2관리자 권한을 일괄 삭제한다")
        void success_DeleteAll() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = adminAccount(1L, "owner@mapo.go.kr");
            AdminAccount first = adminAccount(2L, "sub1@mapo.go.kr");
            AdminAccount second = adminAccount(3L, "sub2@mapo.go.kr");
            List<UUID> adminIds = List.of(first.getPublicId(), second.getPublicId());
            AdminFestivalRole ownerRole = AdminFestivalRole.createFestivalOwner(1L, 1L);
            List<AdminFestivalRole> subAdminRoles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L),
                    AdminFestivalRole.createSubAdmin(3L, 1L, 1L)
            );
            givenOwnerAccess(festival, owner, ownerRole);
            given(adminAccountService.getAllSubAdminsByPublicIds(any()))
                    .willReturn(List.of(first, second));
            given(adminFestivalRoleService.getAllByAdminAccountIdsAndFestivalId(
                    List.of(2L, 3L),
                    1L
            )).willReturn(subAdminRoles);

            // when
            service.deleteAll(festival.getPublicId(), adminIds, principal(owner));

            // then
            then(adminFestivalRoleService).should().deleteAll(subAdminRoles);
        }

        @Test
        @DisplayName("인증 주체가 없으면 삭제할 수 없다")
        void fail_DeleteAll_Unauthorized_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festivalId,
                    List.of(UUID.randomUUID()),
                    null
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("제2관리자는 다른 제2관리자를 삭제할 수 없다")
        void fail_DeleteAll_Forbidden_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount subAdmin = adminAccount(2L, "sub@mapo.go.kr");
            given(adminAccountService.getById(2L)).willReturn(subAdmin);
            given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(2L, 1L))
                    .willReturn(AdminFestivalRole.createSubAdmin(2L, 1L, 1L));

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(UUID.randomUUID()),
                    principal(subAdmin)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("삭제 대상 목록이 비어 있으면 요청을 거절한다")
        void fail_DeleteAll_InvalidRequest_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = adminAccount(1L, "owner@mapo.go.kr");
            givenOwnerAccess(
                    festival,
                    owner,
                    AdminFestivalRole.createFestivalOwner(1L, 1L)
            );

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(),
                    principal(owner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("삭제 대상 UUID가 중복되면 요청을 거절한다")
        void fail_DeleteAll_DuplicatedId_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = adminAccount(1L, "owner@mapo.go.kr");
            UUID adminId = UUID.randomUUID();
            givenOwnerAccess(
                    festival,
                    owner,
                    AdminFestivalRole.createFestivalOwner(1L, 1L)
            );

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(adminId, adminId),
                    principal(owner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("삭제 대상이 100개를 초과하면 요청을 거절한다")
        void fail_DeleteAll_OverMax_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = adminAccount(1L, "owner@mapo.go.kr");
            List<UUID> adminIds = IntStream.range(0, 101)
                    .mapToObj(index -> UUID.randomUUID())
                    .toList();
            givenOwnerAccess(
                    festival,
                    owner,
                    AdminFestivalRole.createFestivalOwner(1L, 1L)
            );

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    adminIds,
                    principal(owner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("다른 제1관리자가 초대한 대상이 섞이면 전체 삭제를 거절한다")
        void fail_DeleteAll_DifferentInviter_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = adminAccount(1L, "owner@mapo.go.kr");
            AdminAccount first = adminAccount(2L, "sub1@mapo.go.kr");
            AdminAccount second = adminAccount(3L, "sub2@mapo.go.kr");
            List<AdminFestivalRole> roles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L),
                    AdminFestivalRole.createSubAdmin(3L, 1L, 99L)
            );
            givenOwnerAccess(
                    festival,
                    owner,
                    AdminFestivalRole.createFestivalOwner(1L, 1L)
            );
            given(adminAccountService.getAllSubAdminsByPublicIds(any()))
                    .willReturn(List.of(first, second));
            given(adminFestivalRoleService.getAllByAdminAccountIdsAndFestivalId(
                    List.of(2L, 3L),
                    1L
            )).willReturn(roles);

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(first.getPublicId(), second.getPublicId()),
                    principal(owner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND.getMessage());
            then(adminFestivalRoleService).should(never()).deleteAll(any());
        }
    }

    private void givenOwnerAccess(
            Festival festival,
            AdminAccount owner,
            AdminFestivalRole ownerRole
    ) {
        given(adminAccountService.getById(owner.getId())).willReturn(owner);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                owner.getId(),
                festival.getId()
        )).willReturn(ownerRole);
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(adminAccount.getId(), adminAccount.getEmailValue());
    }

    private AdminAccount adminAccount(Long id, String email) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("김관리"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", id);
        return adminAccount;
    }

    private Festival festival(Long id) {
        Festival festival = Festival.create(
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
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }
}
