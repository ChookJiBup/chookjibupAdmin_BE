package com.example.chookjibupadmin.report.support.dto;

import java.util.List;

/**
 * 시간대별 방문 패턴 지표이다. 혼잡 이력이 없으면 미제공 상태로 반환한다.
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
