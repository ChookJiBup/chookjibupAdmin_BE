package com.example.chookjibupadmin.report.analysis.application.port;

import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;

/**
 * 축제 결과 보고서 서술·감성 분석을 수행하는 외부 계약이다.
 */
public interface FestivalReportAnalysisPort {

    FestivalReportAiResult analyze(
            FestivalReportMetrics metrics,
            FestivalReviewMetrics reviews
    );
}
