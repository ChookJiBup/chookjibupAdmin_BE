package com.example.chookjibupadmin.report.query.application.dto;

import com.example.chookjibupadmin.report.support.dto.FestivalReportEvaluationAi;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import java.util.UUID;

/**
 * 방문객평가 조회 결과이다.
 */
public record FestivalReportEvaluationView(
        UUID festivalId,
        boolean evaluationAvailable,
        String generationStatus,
        FestivalReviewMetrics reviews,
        FestivalReportEvaluationAi ai
) {
}
