package com.example.chookjibupadmin.dashboard.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.dashboard.query.application.dto.DashboardBoothView;
import com.example.chookjibupadmin.dashboard.query.application.dto.DashboardZoneView;
import com.example.chookjibupadmin.dashboard.query.application.dto.FestivalDashboardView;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 진행 중 대시보드 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalDashboardQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalDashboardMetricProvider metricProvider;

    public FestivalDashboardView getDashboard(
            UUID festivalId,
            FestivalActorPrincipal principal
    ) {
        Festival festival = festivalService.getByPublicId(festivalId);
        authorize(festival, principal);

        return metricProvider.findCurrent(festival.getId())
                .map(metric -> toView(festival.getPublicId(), metric))
                .orElseGet(() -> emptyView(festival.getPublicId()));
    }

    private FestivalDashboardView toView(
            UUID festivalPublicId,
            FestivalDashboardMetricProvider.Snapshot metric
    ) {
        boolean dataAvailable = metric.visitorAvailable()
                || metric.boothAvailable()
                || metric.congestionAvailable()
                || metric.summaryAvailable();
        List<DashboardBoothView> booths = metric.booths().stream()
                .map(b -> new DashboardBoothView(
                        b.boothId(),
                        b.boothName(),
                        b.roadmapNodePublicId(),
                        b.lat(),
                        b.lng(),
                        b.congestionLevel(),
                        b.waitMinutes(),
                        b.congestionCreatedAt(),
                        b.modifierType(),
                        b.modifierAdminId(),
                        b.modifierStaffId(),
                        b.modifierName()
                ))
                .toList();
        List<DashboardZoneView> zones = metric.zones().stream()
                .map(z -> new DashboardZoneView(
                        z.zoneId(),
                        z.name(),
                        z.sortOrder(),
                        z.boothNodeIds()
                ))
                .toList();
        return new FestivalDashboardView(
                festivalPublicId,
                dataAvailable,
                metric.visitorAvailable(),
                metric.boothAvailable(),
                metric.congestionAvailable(),
                metric.summaryAvailable(),
                metric.operatingStatus(),
                metric.currentVisitorCount(),
                metric.activeQueueCount(),
                metric.averageWaitMinutes(),
                metric.updatedAt(),
                booths,
                zones
        );
    }

    private FestivalDashboardView emptyView(UUID festivalPublicId) {
        return new FestivalDashboardView(
                festivalPublicId,
                false,
                false,
                false,
                false,
                false,
                "DATA_UNAVAILABLE",
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private void authorize(Festival festival, FestivalActorPrincipal principal) {
        switch (principal) {
            case AdminPrincipal adminPrincipal -> {
                AdminAccount adminAccount = findAuthenticatedAdmin(adminPrincipal);
                AdminFestivalRole role = adminFestivalRoleService
                        .getByAdminAccountIdAndFestivalId(
                                adminAccount.getId(),
                                festival.getId()
                        );
                if (!role.canViewOperationReport()) {
                    throw new CustomException(ErrorCode.FORBIDDEN);
                }
            }
            case FieldStaffPrincipal staffPrincipal -> {
                if (!festival.getId().equals(staffPrincipal.festivalId())) {
                    throw new CustomException(ErrorCode.FORBIDDEN);
                }
            }
            default -> throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }
}
