package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;
import com.example.chookjibupadmin.map.analysis.application.port.MapBlueprintAnalysisPort;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.map.analysis",
        name = "provider",
        havingValue = "openai"
)
public class MapAnalysisWorker {

    private final MapAnalysisJobService jobService;
    private final FestivalMapService mapService;
    private final MapImageStoragePort storagePort;
    private final MapBlueprintAnalysisPort analysisPort;
    private final MapAnalysisResultApplicationService resultService;
    private final MapAnalysisProperties properties;

    @Scheduled(fixedDelayString = "${app.map.analysis.poll-delay-millis:3000}")
    public void processNext() {
        jobService.claimPending().ifPresent(this::process);
    }

    void process(MapAnalysisJob job) {
        try {
            FestivalMap map = mapService.getById(job.getMapId());
            byte[] image = storagePort.read(
                    job.getInputImageKey(),
                    properties.maxInputBytesOrDefault()
            );
            MapAnalysisResult result = analysisPort.analyze(
                    image,
                    map.getAnalysisContentType().getValue(),
                    job.getInputImageWidth(),
                    job.getInputImageHeight()
            );

            resultService.complete(job.getPublicId(), result);
        } catch (MapAnalysisException exception) {
            handleFailure(
                    job,
                    exception.code(),
                    exception.getMessage(),
                    exception.retryable()
            );
        } catch (RuntimeException exception) {
            handleFailure(
                    job,
                    "ANALYSIS_INTERNAL_ERROR",
                    "Map analysis failed",
                    true
            );
        }
    }

    private void handleFailure(
            MapAnalysisJob job,
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
        log.warn(
                "Map analysis job failed: jobId={}, code={}, retryable={}",
                job.getPublicId(),
                code,
                retryable
        );
    }
}
