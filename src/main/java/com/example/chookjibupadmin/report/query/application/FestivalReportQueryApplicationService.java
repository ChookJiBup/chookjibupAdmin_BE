package com.example.chookjibupadmin.report.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.query.application.dto.FestivalReportSummaryView;
import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 종료 후 결과 보고서 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalReportQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalReportMetricProvider metricProvider;

    /**
     * 담당 축제의 결과 보고서 요약 정보를 조회한다.
     */
    public FestivalReportSummaryView getSummary(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateReportAccess(festival, adminAccount);

        return metricProvider.findSummary(festival.getId())
                .map(metric -> new FestivalReportSummaryView(
                        festival.getPublicId(), true,
                        metric.totalVisitorCount(),
                        metric.peakConcurrentVisitorCount(),
                        metric.averageWaitMinutes(),
                        metric.generatedAt()
                ))
                .orElseGet(() -> new FestivalReportSummaryView(
                        festival.getPublicId(), false,
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
