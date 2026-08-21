package com.example.chookjibupadmin.report.query.application.dto;

import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import java.util.UUID;

/**
 * 축제성과 조회 결과이다.
 */
public record FestivalReportPerformanceView(
        UUID festivalId,
        boolean performanceAvailable,
        String generationStatus,
        FestivalReportMetrics metrics,
        FestivalReportAiResult ai
) {
}
