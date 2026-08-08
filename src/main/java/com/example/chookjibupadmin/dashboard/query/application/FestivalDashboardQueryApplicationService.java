package com.example.chookjibupadmin.dashboard.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.dashboard.query.application.dto.FestivalDashboardView;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
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

    /**
     * 담당 축제의 진행 중 대시보드 요약 정보를 조회한다.
     */
    public FestivalDashboardView getDashboard(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateReportAccess(festival, adminAccount);

        return metricProvider.findCurrent(festival.getId())
                .map(metric -> new FestivalDashboardView(
                        festival.getPublicId(), true, metric.operatingStatus(),
                        metric.currentVisitorCount(), metric.activeQueueCount(),
                        metric.averageWaitMinutes(), metric.updatedAt()
                ))
                .orElseGet(() -> new FestivalDashboardView(
                        festival.getPublicId(), false, "DATA_UNAVAILABLE",
                        0L, 0L, 0L, null
                ));
    }

    private void validateReportAccess(
            Festival festival,
            AdminAccount adminAccount
    ) {
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canViewOperationReport()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }
}
