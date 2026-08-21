package com.example.chookjibupadmin.report.analysis.infrastructure;

import com.example.chookjibupadmin.report.analysis.application.port.FestivalReportAnalysisPort;
import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI 연동이 꺼져 있을 때 빈 AI 결과를 반환한다.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.report.analysis",
        name = "provider",
        havingValue = "disabled",
        matchIfMissing = true
)
public class NoOpFestivalReportAnalysisAdapter
        implements FestivalReportAnalysisPort {

    @Override
    public FestivalReportAiResult analyze(
            FestivalReportMetrics metrics,
            FestivalReviewMetrics reviews
    ) {
        return FestivalReportAiResult.empty();
    }
}
