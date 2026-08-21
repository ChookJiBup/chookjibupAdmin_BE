package com.example.chookjibupadmin.report.support.dto;

/**
 * OpenAI가 생성한 결과 보고서 서술·감성 결과이다.
 */
public record FestivalReportAiResult(
        FestivalReportTextSummary performanceSummary,
        FestivalReportEvaluationAi evaluation
) {

    public static FestivalReportAiResult empty() {
        return new FestivalReportAiResult(
                FestivalReportTextSummary.empty(),
                FestivalReportEvaluationAi.empty()
        );
    }
}
