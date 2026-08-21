package com.example.chookjibupadmin.api.report.dto;

import com.example.chookjibupadmin.report.query.application.dto.FestivalReportPerformanceView;
import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import java.util.UUID;

public record FestivalReportPerformanceResponse(
        UUID festivalId,
        boolean performanceAvailable,
        String generationStatus,
        FestivalReportMetrics metrics,
        FestivalReportAiResult ai
) {

    public static FestivalReportPerformanceResponse from(
            FestivalReportPerformanceView view
    ) {
        return new FestivalReportPerformanceResponse(
                view.festivalId(),
                view.performanceAvailable(),
                view.generationStatus(),
                view.metrics(),
                view.ai()
        );
    }
}
