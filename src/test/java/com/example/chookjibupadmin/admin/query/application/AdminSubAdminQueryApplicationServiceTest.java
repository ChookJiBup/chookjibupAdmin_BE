package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminSubAdminQueryApplicationServiceTest {

    @InjectMocks
    private AdminSubAdminQueryApplicationService applicationService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminSubAdminQueryService subAdminQueryService;

    @Mock
    private AdminNameEmailSearchMatcher searchMatcher;

    @Nested
    @DisplayName("getSubAdmins")
    class GetSubAdmins {

        @Test
        @DisplayName("제1 관리자가 담당 축제의 서브관리자 목록을 조회한다")
        void success_GetSubAdmins() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = owner(festival.getId());
            AdminPrincipal principal = principal(owner);
            AdminSubAdminView view = subAdminView();
            given(adminAccountService.getById(principal.adminId())).willReturn(owner);
            given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
            givenOwnerRole(owner.getId(), festival.getId());
            given(subAdminQueryService.findInvitedSubAdmins(
                    festival.getId(),
                    owner.getId()
            ))
                    .willReturn(List.of(view));
            given(searchMatcher.search(List.of(view), "김"))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminView> result = applicationService.getSubAdmins(
                    festival.getPublicId(),
                    "김",
                    principal
            );

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_GetSubAdmins_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() ->
                    applicationService.getSubAdmins(festivalId, null, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("제2 관리자는 서브관리자 목록을 조회할 수 없다")
        void fail_GetSubAdmins_SubAdmin_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount subAdmin = subAdmin(festival.getId());
            AdminPrincipal principal = principal(subAdmin);
            given(adminAccountService.getById(principal.adminId())).willReturn(subAdmin);
            given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    subAdmin.getId(),
                    festival.getId()
            )).willReturn(AdminFestivalRole.createSubAdmin(
                    subAdmin.getId(),
                    festival.getId(),
                    1L
            ));

            // when & then
            assertThatThrownBy(() ->
                    applicationService.getSubAdmins(festival.getPublicId(), null, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("다른 축제의 제1 관리자는 서브관리자 목록을 조회할 수 없다")
        void fail_GetSubAdmins_DifferentFestival_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = owner(2L);
            AdminPrincipal principal = principal(owner);
            given(adminAccountService.getById(principal.adminId())).willReturn(owner);
            given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    owner.getId(),
                    festival.getId()
            )).willThrow(new CustomException(ErrorCode.FORBIDDEN));

            // when & then
            assertThatThrownBy(() ->
                    applicationService.getSubAdmins(festival.getPublicId(), null, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("getSubAdmin")
    class GetSubAdmin {

        @Test
        @DisplayName("제1 관리자가 담당 축제의 서브관리자를 단건 조회한다")
        void success_GetSubAdmin() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = owner(festival.getId());
            AdminPrincipal principal = principal(owner);
            UUID subAdminId = UUID.randomUUID();
            AdminSubAdminView view = subAdminView(subAdminId);
            given(adminAccountService.getById(principal.adminId())).willReturn(owner);
            given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
            givenOwnerRole(owner.getId(), festival.getId());
            given(subAdminQueryService.getInvitedSubAdmin(
                    festival.getId(),
                    owner.getId(),
                    subAdminId
            )).willReturn(view);

            // when
            AdminSubAdminView result = applicationService.getSubAdmin(
                    festival.getPublicId(),
                    subAdminId,
                    principal
            );

            // then
            assertThat(result).isEqualTo(view);
        }
    }

    private AdminAccount owner(Long festivalId) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
    }

    private AdminAccount subAdmin(Long festivalId) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("sub@mapo.go.kr"),
                AdminName.of("김관리"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 2L);
        return adminAccount;
    }

    private void givenOwnerRole(Long adminId, Long festivalId) {
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                adminId,
                festivalId
        )).willReturn(AdminFestivalRole.createFestivalOwner(adminId, festivalId));
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private Festival festival(Long festivalId) {
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
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        ReflectionTestUtils.setField(festival, "id", festivalId);
        return festival;
    }

    private AdminSubAdminView subAdminView() {
        return subAdminView(UUID.randomUUID());
    }

    private AdminSubAdminView subAdminView(UUID adminId) {
        return new AdminSubAdminView(
                adminId,
                "sub@mapo.go.kr",
                "김관리",
                "마포구청 소속",
                "관광정책과",
                "주무관",
                AdminStatus.ACTIVE
        );
    }
}
