package com.example.chookjibupadmin.report.support.dto;

import java.util.List;
import java.util.UUID;

/**
 * 축제 성과 보고서에 사용하는 서버 집계 지표 묶음이다.
 */
public record FestivalReportMetrics(
        UUID festivalId,
        String festivalName,
        int festivalYear,
        int totalDayCount,
        boolean visitorInputCompleted,
        FestivalTotalVisitorMetric totalVisitors,
        List<FestivalDailyVisitorTrendPoint> dailyTrend,
        FestivalEconomicEffectMetric economicEffect,
        FestivalOperationEfficiencyMetric operationEfficiency,
        List<FestivalZoneWaitRankingItem> zoneWaitRanking,
        List<FestivalBoothCongestionShareItem> boothCongestionShare,
        FestivalVisitPatternMetric visitPattern
) {
}
