package com.example.chookjibupadmin.report.support.dto;

import java.util.List;

/**
 * 방문객 평가용 OpenAI 생성 결과이다.
 */
public record FestivalReportEvaluationAi(
        String headlineSentiment,
        List<String> positiveKeywords,
        List<String> negativeKeywords,
        FestivalReportTextSummary summary
) {

    public static FestivalReportEvaluationAi empty() {
        return new FestivalReportEvaluationAi(
                "NONE",
                List.of(),
                List.of(),
                FestivalReportTextSummary.empty()
        );
    }
}
