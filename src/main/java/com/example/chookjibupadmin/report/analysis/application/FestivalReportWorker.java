package com.example.chookjibupadmin.report.analysis.application;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.report.analysis.application.port.FestivalReportAnalysisPort;
import com.example.chookjibupadmin.report.analysis.infrastructure.openai.ReportAnalysisProperties;
import com.example.chookjibupadmin.report.command.application.FestivalReportJobService;
import com.example.chookjibupadmin.report.command.application.FestivalResultService;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalResult;
import com.example.chookjibupadmin.report.query.infrastructure.FestivalReviewMetricQueryRepository;
import com.example.chookjibupadmin.report.support.FestivalReportMetricAssembler;
import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalReportWorker {

    private static final String INTERNAL_ERROR_CODE = "REPORT_INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_MESSAGE =
            "결과 보고서 생성 중 내부 오류가 발생했습니다.";

    private final FestivalReportJobService jobService;
    private final FestivalResultService resultService;
    private final FestivalService festivalService;
    private final FestivalReportMetricAssembler metricAssembler;
    private final FestivalReviewMetricQueryRepository reviewMetricQueryRepository;
    private final FestivalReportAnalysisPort analysisPort;
    private final ReportAnalysisProperties properties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.report.analysis.poll-delay-millis:3000}")
    public void processNext() {
        jobService.claimPending().ifPresent(this::process);
    }

    /**
     * 제한 시간을 넘겨 처리 중에 갇힌 작업을 재시도 대기 또는 실패로 회수한다.
     *
     * <p>애플리케이션이 분석 도중 재시작되면 작업은 다시 선택되지 못한 채
     * PROCESSING으로 남으므로, 폴링과 별도로 회수 경로를 둔다.</p>
     */
    @Scheduled(
            fixedDelayString =
                    "${app.report.analysis.timeout-scan-delay-millis:30000}"
    )
    public void releaseTimedOutJobs() {
        jobService.releaseTimedOut(
                properties.jobTimeoutOrDefault(),
                properties.maxAttemptsOrDefault()
        ).forEach(released -> log.warn(
                "Festival report job timed out: jobId={}, festivalId={}, "
                        + "timeout={}, retrying={}",
                released.jobPublicId(),
                released.festivalId(),
                properties.jobTimeoutOrDefault(),
                released.retrying()
        ));
    }

    void process(FestivalReportJob job) {
        UUID jobId = job.getPublicId();
        long startedAt = System.currentTimeMillis();
        log.info(
                "Festival report job started: jobId={}, festivalId={}, "
                        + "provider={}, model={}, attempt={}",
                jobId,
                job.getFestivalId(),
                job.getProvider(),
                job.getModel(),
                job.getAttemptCount()
        );

        try {
            Festival festival = festivalService.getById(job.getFestivalId());
            FestivalReportMetrics metrics = metricAssembler.assemble(festival);
            Long previousFestivalId = null;
            if (festival.getSeriesId() != null && festival.getYear() != null) {
                previousFestivalId = festivalService
                        .findBySeriesIdAndYear(
                                festival.getSeriesId(),
                                festival.getYear() - 1
                        )
                        .map(Festival::getId)
                        .orElse(null);
            }

            for (var point : metrics.dailyTrend()) {
                jobService.updateProgress(
                        jobId,
                        point.dayIndex(),
                        point.dayIndex() + "일차 분석 중"
                );
            }

            FestivalReviewMetrics reviews =
                    reviewMetricQueryRepository.findByFestivalId(
                            festival.getId(),
                            previousFestivalId
                    );
            log.info(
                    "Festival report analysis requested: jobId={}, provider={}, "
                            + "model={}, days={}, reviews={}",
                    jobId,
                    job.getProvider(),
                    job.getModel(),
                    metrics.dailyTrend().size(),
                    reviews.reviewCount()
            );
            FestivalReportAiResult aiResult = analysisPort.analyze(
                    metrics,
                    reviews
            );
            log.info(
                    "Festival report analysis responded: jobId={}, elapsedMs={}",
                    jobId,
                    System.currentTimeMillis() - startedAt
            );

            String metricsJson = objectMapper.writeValueAsString(metrics);
            String aiJson = objectMapper.writeValueAsString(aiResult);
            FestivalResult result = resultService
                    .findByFestivalId(festival.getId())
                    .map(existing -> {
                        existing.replace(
                                metricsJson,
                                aiJson,
                                job.getSchemaVersion(),
                                "COMPLETED"
                        );
                        return existing;
                    })
                    .orElseGet(() -> FestivalResult.create(
                            festival.getId(),
                            metricsJson,
                            aiJson,
                            job.getSchemaVersion(),
                            "COMPLETED"
                    ));
            resultService.save(result);

            if (jobService.complete(jobId)) {
                log.info(
                        "Festival report job completed: jobId={}, elapsedMs={}",
                        jobId,
                        System.currentTimeMillis() - startedAt
                );
            } else {
                log.warn(
                        "Festival report job is no longer processing so it was "
                                + "not completed: jobId={}",
                        jobId
                );
            }
        } catch (FestivalReportAnalysisException exception) {
            handleFailure(
                    job,
                    exception.code(),
                    exception.getMessage(),
                    exception.retryable(),
                    exception
            );
        } catch (Exception exception) {
            handleFailure(
                    job,
                    INTERNAL_ERROR_CODE,
                    INTERNAL_ERROR_MESSAGE,
                    true,
                    exception
            );
        }
    }

    private void handleFailure(
            FestivalReportJob job,
            String code,
            String message,
            boolean retryable,
            Exception cause
    ) {
        try {
            boolean retrying = jobService.recordFailure(
                    job.getPublicId(),
                    code,
                    message,
                    retryable,
                    properties.maxAttemptsOrDefault()
            );
            log.warn(
                    "Festival report job failed: jobId={}, festivalId={}, "
                            + "code={}, retryable={}, retrying={}",
                    job.getPublicId(),
                    job.getFestivalId(),
                    code,
                    retryable,
                    retrying,
                    cause
            );
        } catch (RuntimeException failureRecordingError) {
            // 실패 기록까지 실패하면 작업이 처리 중으로 남으므로 회수 스케줄러에 맡긴다.
            log.error(
                    "Festival report job failure could not be recorded: "
                            + "jobId={}, code={}",
                    job.getPublicId(),
                    code,
                    failureRecordingError
            );
        }
    }
}
