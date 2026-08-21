package com.example.chookjibupadmin.api.report.dto;

import com.example.chookjibupadmin.report.query.application.dto.FestivalReportEvaluationView;
import com.example.chookjibupadmin.report.support.dto.FestivalReportEvaluationAi;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import java.util.UUID;

public record FestivalReportEvaluationResponse(
        UUID festivalId,
        boolean evaluationAvailable,
        String generationStatus,
        FestivalReviewMetrics reviews,
        FestivalReportEvaluationAi ai
) {

    public static FestivalReportEvaluationResponse from(
            FestivalReportEvaluationView view
    ) {
        return new FestivalReportEvaluationResponse(
                view.festivalId(),
                view.evaluationAvailable(),
                view.generationStatus(),
                view.reviews(),
                view.ai()
        );
    }
}
