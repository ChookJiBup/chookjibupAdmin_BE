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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalReportWorker {

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

    void process(FestivalReportJob job) {
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
                job.updateProgress(
                        point.dayIndex(),
                        point.dayIndex() + "일차 분석 중"
                );
                jobService.save(job);
            }

            FestivalReviewMetrics reviews =
                    reviewMetricQueryRepository.findByFestivalId(
                            festival.getId(),
                            previousFestivalId
                    );
            FestivalReportAiResult aiResult = analysisPort.analyze(
                    metrics,
                    reviews
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
            job.complete();
            jobService.save(job);
        } catch (FestivalReportAnalysisException exception) {
            handleFailure(
                    job,
                    exception.code(),
                    exception.getMessage(),
                    exception.retryable()
            );
        } catch (Exception exception) {
            handleFailure(
                    job,
                    "REPORT_INTERNAL_ERROR",
                    "Festival report analysis failed",
                    true
            );
            log.warn(
                    "Festival report job failed: jobId={}",
                    job.getPublicId(),
                    exception
            );
        }
    }

    private void handleFailure(
            FestivalReportJob job,
            String code,
            String message,
            boolean retryable
    ) {
        if (retryable
                && job.getAttemptCount() < properties.maxAttemptsOrDefault()) {
            job.retry(code, message);
        } else {
            job.fail(code, message);
        }
        jobService.save(job);
    }
}
