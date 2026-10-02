package com.example.chookjibupadmin.report.support.dto;

import java.util.List;

/**
 * 혼잡 이력의 평균 대기시간으로 계산한 주요 혼잡 시간대 지표이다.
 */
public record FestivalVisitPatternMetric(
        boolean available,
        List<String> peakHours
) {

    /**
     * 시간대 이력이 없어 제공할 수 없는 방문 패턴 지표를 만든다.
     */
    public static FestivalVisitPatternMetric unavailable() {
        return new FestivalVisitPatternMetric(false, List.of());
    }
}
