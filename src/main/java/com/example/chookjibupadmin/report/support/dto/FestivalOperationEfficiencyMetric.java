package com.example.chookjibupadmin.report.support.dto;

/**
 * 평균 대기 시간과 부스 수 기반 운영효율 지표이다.
 */
public record FestivalOperationEfficiencyMetric(
        boolean available,
        Long averageWaitMinutes,
        int boothCount
) {

    /**
     * 혼잡 이력이 없어 제공할 수 없는 운영효율 지표를 만든다.
     */
    public static FestivalOperationEfficiencyMetric unavailable() {
        return new FestivalOperationEfficiencyMetric(false, null, 0);
    }
}
