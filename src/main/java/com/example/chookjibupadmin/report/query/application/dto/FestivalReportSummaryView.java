package com.example.chookjibupadmin.report.query.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FestivalReportSummaryView(
        UUID festivalId,
        boolean dataAvailable,
        long totalVisitorCount,
        long peakConcurrentVisitorCount,
        long averageWaitMinutes,
        LocalDateTime generatedAt
) {
}
