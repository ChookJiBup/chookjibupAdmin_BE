package com.example.chookjibupadmin.report.support.dto;

import java.math.BigDecimal;

/**
 * 혼잡도 등급별 부스 비율 항목이다.
 */
public record FestivalBoothCongestionShareItem(
        String congestionLevel,
        BigDecimal sharePercent
) {
}
