package com.example.chookjibupadmin.report.query.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결과 보고서 상태 조회 결과이다.
 */
public record FestivalReportStatusView(
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
}
