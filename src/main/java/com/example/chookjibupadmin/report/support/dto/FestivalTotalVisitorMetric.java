package com.example.chookjibupadmin.report.support.dto;

import java.math.BigDecimal;

/**
 * 총 관광객 수와 전년 대비 증감 지표이다.
 */
public record FestivalTotalVisitorMetric(
        long current,
        Long previous,
        Long delta,
        BigDecimal changeRatePercent,
        FestivalVisitorChangeDirection direction
) {
}
