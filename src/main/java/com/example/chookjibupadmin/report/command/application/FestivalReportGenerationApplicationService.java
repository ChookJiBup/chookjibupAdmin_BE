package com.example.chookjibupadmin.report.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.analysis.infrastructure.openai.ReportAnalysisProperties;
import com.example.chookjibupadmin.report.command.application.dto.FestivalReportGenerateResult;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputStatus;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 결과 보고서 생성 작업을 등록한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalReportGenerationApplicationService {

    private static final String PROMPT_VERSION = "1.0";
    private static final String SCHEMA_VERSION = "1.0";

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final FestivalReportJobService reportJobService;
    private final ReportAnalysisProperties reportAnalysisProperties;

    public FestivalReportGenerateResult generate(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        AdminAccount admin = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        admin.getId(),
                        festival.getId()
                );
        if (!role.canViewOperationReport()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        ensureVisitorInputCompleted(festival);
        FestivalReportJob job = enqueueReplacingActive(festival.getId());
        return new FestivalReportGenerateResult(
                festival.getPublicId(),
                job.getPublicId(),
                job.getStatus().name()
        );
    }

    /**
     * 방문 인원 입력이 끝난 뒤 자동 생성 잡을 등록한다.
     */
    public void enqueueIfReady(Long festivalId) {
        Festival festival = festivalService.getById(festivalId);
        if (!isVisitorInputReady(festival)) {
            return;
        }
        if (reportJobService.existsActive(festivalId)) {
            return;
        }
        enqueueReplacingActive(festivalId);
    }

    private FestivalReportJob enqueueReplacingActive(Long festivalId) {
        reportJobService.cancelActive(festivalId);
        FestivalReportJob job = FestivalReportJob.pending(
                festivalId,
                reportAnalysisProperties.providerOrDefault(),
                reportAnalysisProperties.modelOrDefault(),
                PROMPT_VERSION,
                SCHEMA_VERSION
        );
        return reportJobService.save(job);
    }

    private void ensureVisitorInputCompleted(Festival festival) {
        var snapshot = resolveSnapshot(festival);
        if (snapshot.status() == FestivalVisitorInputStatus.CONFLICT) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_REPORT_VISITOR_INPUT_CONFLICT
            );
        }
        if (!FestivalVisitorInputSupport.isReportReady(snapshot)) {
            throw new CustomException(
                    ErrorCode.FESTIVAL_REPORT_VISITOR_INPUT_INCOMPLETE
            );
        }
    }

    private boolean isVisitorInputReady(Festival festival) {
        return FestivalVisitorInputSupport.isReportReady(resolveSnapshot(festival));
    }

    private FestivalVisitorInputSupport.FestivalVisitorInputSnapshot resolveSnapshot(
            Festival festival
    ) {
        return FestivalVisitorInputSupport.resolve(
                festival,
                visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(
                        festival.getId()
                ),
                visitorCountService.findTotalByFestivalId(festival.getId())
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }
}
