package com.example.chookjibupadmin.api.report.dto;

import com.example.chookjibupadmin.report.query.application.dto.FestivalReportStatusView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "축제 결과 보고서 상태 응답")
public record FestivalReportStatusResponse(
        UUID festivalId,
        String progressStatus,
        String visitorInput,
        String generationStatus,
        Integer progressDayIndex,
        String progressMessage,
        boolean performanceAvailable,
        boolean evaluationAvailable,
        UUID previousFestivalId,
        LocalDateTime generatedAt,
        UUID jobId
) {

    public static FestivalReportStatusResponse from(
            FestivalReportStatusView view
    ) {
        return new FestivalReportStatusResponse(
                view.festivalId(),
                view.progressStatus(),
                view.visitorInput(),
                view.generationStatus(),
                view.progressDayIndex(),
                view.progressMessage(),
                view.performanceAvailable(),
                view.evaluationAvailable(),
                view.previousFestivalId(),
                view.generatedAt(),
                view.jobId()
        );
    }
}
