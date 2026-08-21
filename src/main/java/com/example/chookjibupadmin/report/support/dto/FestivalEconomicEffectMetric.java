package com.example.chookjibupadmin.report.support.dto;

import java.math.BigDecimal;

/**
 * 경제효과 지표이다. 산출 공식이 확정되기 전에는 미제공 상태로 반환한다.
 */
public record FestivalEconomicEffectMetric(
        boolean available,
        BigDecimal totalMillionKrw,
        BigDecimal previousMillionKrw
) {

    /**
     * 산출 근거가 없어 제공할 수 없는 경제효과 지표를 만든다.
     */
    public static FestivalEconomicEffectMetric unavailable() {
        return new FestivalEconomicEffectMetric(false, null, null);
    }
}
