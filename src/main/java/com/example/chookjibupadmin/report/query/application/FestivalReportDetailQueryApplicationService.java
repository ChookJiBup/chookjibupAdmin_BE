package com.example.chookjibupadmin.report.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.command.application.FestivalReportJobService;
import com.example.chookjibupadmin.report.command.application.FestivalResultService;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import com.example.chookjibupadmin.report.query.application.dto.FestivalReportEvaluationView;
import com.example.chookjibupadmin.report.query.application.dto.FestivalReportPerformanceView;
import com.example.chookjibupadmin.report.query.application.dto.FestivalReportStatusView;
import com.example.chookjibupadmin.report.query.infrastructure.FestivalReviewMetricQueryRepository;
import com.example.chookjibupadmin.report.support.FestivalReportMetricAssembler;
import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportEvaluationAi;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalReportTextSummary;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 결과 보고서 상세 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalReportDetailQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final FestivalReportJobService reportJobService;
    private final FestivalResultService resultService;
    private final FestivalReportMetricAssembler metricAssembler;
    private final FestivalReviewMetricQueryRepository reviewMetricQueryRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public FestivalReportStatusView getStatus(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        Festival festival = authorize(festivalPublicId, principal);
        List<FestivalDailyVisitorCount> dailyCounts = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                dailyCounts,
                visitorCountService.findTotalByFestivalId(festival.getId())
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
        boolean visitorReady = FestivalVisitorInputSupport.isReportReady(snapshot);
        String visitorInput = switch (snapshot.status()) {
            case READY -> "COMPLETE";
            case CONFLICT -> "CONFLICT";
            case UNSET -> "MISSING";
            case PARTIAL -> "PARTIAL";
        };

        Optional<FestivalReportJob> job = reportJobService.findLatestByFestivalId(
                festival.getId()
        );
        Optional<FestivalResult> result = resultService.findByFestivalId(
                festival.getId()
        );

        FestivalProgressStatus progress = FestivalProgressStatus.from(
                LocalDate.now(clock),
                festival.getStartDate(),
                festival.getEndDate()
        );

        UUID previousFestivalId = null;
        if (festival.getSeriesId() != null && festival.getYear() != null) {
            previousFestivalId = festivalService
                    .findBySeriesIdAndYear(
                            festival.getSeriesId(),
                            festival.getYear() - 1
                    )
                    .map(Festival::getPublicId)
                    .orElse(null);
        }

        String generationStatus = job.map(value -> value.getStatus().name())
                .orElse(result.isPresent() ? "COMPLETED" : "NONE");

        FestivalReportAiResult ai = result.map(this::readAi)
                .orElseGet(FestivalReportAiResult::empty);
        boolean jobFailed = job
                .map(value -> value.getStatus() == FestivalReportJobStatus.FAILED)
                .orElse(false);
        boolean performanceAvailable = visitorReady
                && !jobFailed
                && result.isPresent();
        boolean evaluationAvailable = visitorReady
                && !jobFailed
                && hasEvaluationContent(ai);

        return new FestivalReportStatusView(
                festival.getPublicId(),
                progress.name(),
                visitorInput,
                generationStatus,
                job.map(FestivalReportJob::getProgressDayIndex).orElse(null),
                job.map(FestivalReportJob::getProgressMessage).orElse(null),
                performanceAvailable,
                evaluationAvailable,
                previousFestivalId,
                result.map(FestivalResult::getGeneratedAt).orElse(null),
                job.map(FestivalReportJob::getPublicId).orElse(null)
        );
    }

    public FestivalReportPerformanceView getPerformance(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        Festival festival = authorize(festivalPublicId, principal);
        FestivalReportMetrics metrics = metricAssembler.assemble(festival);
        Optional<FestivalReportJob> job = reportJobService.findLatestByFestivalId(
                festival.getId()
        );
        Optional<FestivalResult> result = resultService.findByFestivalId(festival.getId());
        FestivalReportAiResult ai = result
                .map(this::readAi)
                .orElseGet(FestivalReportAiResult::empty);
        String generationStatus = job.map(value -> value.getStatus().name())
                .orElse(result.isPresent() ? "COMPLETED" : "NONE");
        boolean jobFailed = job
                .map(value -> value.getStatus() == FestivalReportJobStatus.FAILED)
                .orElse(false);
        boolean performanceAvailable = metrics.visitorInputCompleted()
                && !jobFailed
                && result.isPresent();

        return new FestivalReportPerformanceView(
                festival.getPublicId(),
                performanceAvailable,
                generationStatus,
                metrics,
                ai
        );
    }

    public FestivalReportEvaluationView getEvaluation(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        Festival festival = authorize(festivalPublicId, principal);
        Long previousId = null;
        if (festival.getSeriesId() != null && festival.getYear() != null) {
            previousId = festivalService
                    .findBySeriesIdAndYear(
                            festival.getSeriesId(),
                            festival.getYear() - 1
                    )
                    .map(Festival::getId)
                    .orElse(null);
        }
        FestivalReviewMetrics reviews =
                reviewMetricQueryRepository.findByFestivalId(
                        festival.getId(),
                        previousId
                );
        FestivalReportAiResult ai = resultService
                .findByFestivalId(festival.getId())
                .map(this::readAi)
                .orElse(FestivalReportAiResult.empty());
        Optional<FestivalReportJob> job = reportJobService.findLatestByFestivalId(
                festival.getId()
        );
        boolean available = FestivalVisitorInputSupport.isReportReady(
                FestivalVisitorInputSupport.resolve(
                        festival,
                        visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(
                                festival.getId()
                        ),
                        visitorCountService.findTotalByFestivalId(festival.getId())
                                .map(FestivalTotalVisitorCount::getVisitorCountValue)
                )
        )
                && job.map(value -> value.getStatus() != FestivalReportJobStatus.FAILED)
                        .orElse(true)
                && hasEvaluationContent(ai);

        return new FestivalReportEvaluationView(
                festival.getPublicId(),
                available,
                job.map(value -> value.getStatus().name()).orElse("NONE"),
                reviews,
                ai.evaluation() == null
                        ? FestivalReportEvaluationAi.empty()
                        : ai.evaluation()
        );
    }

    private Festival authorize(
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
        return festival;
    }

    private FestivalReportAiResult readAi(FestivalResult result) {
        if (result.getAiJson() == null || result.getAiJson().isBlank()) {
            return FestivalReportAiResult.empty();
        }
        try {
            return objectMapper.readValue(
                    result.getAiJson(),
                    FestivalReportAiResult.class
            );
        } catch (Exception exception) {
            return FestivalReportAiResult.empty();
        }
    }

    private boolean hasEvaluationContent(FestivalReportAiResult ai) {
        FestivalReportEvaluationAi evaluation = ai.evaluation();
        if (evaluation == null) {
            return false;
        }
        FestivalReportTextSummary summary = evaluation.summary();
        boolean hasKeywords = !evaluation.positiveKeywords().isEmpty()
                || !evaluation.negativeKeywords().isEmpty();
        boolean hasSummary = summary != null
                && !(summary.positives().isEmpty()
                && summary.issues().isEmpty()
                && summary.improvements().isEmpty());
        boolean hasSentiment = evaluation.headlineSentiment() != null
                && !"NONE".equals(evaluation.headlineSentiment());
        return hasSentiment || hasKeywords || hasSummary;
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }
}
