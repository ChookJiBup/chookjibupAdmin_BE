package com.example.chookjibupadmin.dashboard.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.dashboard.query.application.dto.FestivalDashboardView;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
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
class FestivalDashboardQueryApplicationServiceTest {

    @Mock
    private FestivalDashboardMetricProvider metricProvider;

    @InjectMocks
    private FestivalDashboardQueryApplicationService dashboardQueryService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("담당 축제의 진행 중 대시보드를 조회한다")
        void success_GetDashboard_FestivalOwner() {
            Long festivalId = 1L;
            Festival festival = festival(festivalId);
            UUID publicId = festival.getPublicId();
            AdminPrincipal principal = principal();
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, festivalId))
                    .willReturn(AdminFestivalRole.createFestivalOwner(1L, festivalId));
            given(metricProvider.findCurrent(festivalId)).willReturn(Optional.empty());

            FestivalDashboardView view = dashboardQueryService.getDashboard(
                    publicId,
                    principal
            );

            assertThat(view.festivalId()).isEqualTo(publicId);
            assertThat(view.dataAvailable()).isFalse();
            assertThat(view.visitorAvailable()).isFalse();
            assertThat(view.currentVisitorCount()).isNull();
            assertThat(view.operatingStatus()).isEqualTo("DATA_UNAVAILABLE");
            assertThat(view.booths()).isEmpty();
            assertThat(view.zones()).isEmpty();
        }

        @Test
        @DisplayName("배정된 스태프는 대시보드를 조회할 수 있다")
        void success_GetDashboard_FieldStaff() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            FieldStaffPrincipal staff = new FieldStaffPrincipal(5L, 10L, "staff1", 0L);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(metricProvider.findCurrent(10L)).willReturn(Optional.empty());

            FestivalDashboardView view = dashboardQueryService.getDashboard(publicId, staff);

            assertThat(view.festivalId()).isEqualTo(publicId);
            assertThat(view.visitorAvailable()).isFalse();
            assertThat(view.currentVisitorCount()).isNull();
        }

        @Test
        @DisplayName("다른 축제 스태프는 대시보드를 조회할 수 없다")
        void fail_GetDashboard_DifferentFestivalStaff() {
            Festival festival = festival(10L);
            UUID publicId = festival.getPublicId();
            FieldStaffPrincipal staff = new FieldStaffPrincipal(5L, 99L, "staff1", 0L);
            given(festivalService.getByPublicId(publicId)).willReturn(festival);

            assertThatThrownBy(() -> dashboardQueryService.getDashboard(publicId, staff))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("다른 축제의 진행 중 대시보드는 조회할 수 없다")
        void fail_GetDashboard_DifferentFestival_CustomException() {
            Festival festival = festival(1L);
            UUID publicId = festival.getPublicId();
            AdminPrincipal principal = principal();
            given(festivalService.getByPublicId(publicId)).willReturn(festival);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(unassignedAdmin());
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 1L))
                    .willThrow(new CustomException(ErrorCode.FORBIDDEN));

            assertThatThrownBy(() -> dashboardQueryService.getDashboard(publicId, principal))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "owner@mapo.go.kr");
    }

    private AdminAccount unassignedAdmin() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
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
}
