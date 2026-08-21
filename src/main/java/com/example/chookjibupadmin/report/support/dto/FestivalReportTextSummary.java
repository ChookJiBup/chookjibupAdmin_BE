package com.example.chookjibupadmin.report.support.dto;

import java.util.List;

/**
 * 긍정·미흡·개선 문장 묶음이다.
 */
public record FestivalReportTextSummary(
        List<String> positives,
        List<String> issues,
        List<String> improvements
) {

    public static FestivalReportTextSummary empty() {
        return new FestivalReportTextSummary(List.of(), List.of(), List.of());
    }
}
