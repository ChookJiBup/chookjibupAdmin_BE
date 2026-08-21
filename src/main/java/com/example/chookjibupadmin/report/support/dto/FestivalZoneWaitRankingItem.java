package com.example.chookjibupadmin.report.support.dto;

/**
 * 구역별 평균 대기 시간 랭킹 항목이다.
 */
public record FestivalZoneWaitRankingItem(
        int rank,
        String zoneName,
        long averageWaitMinutes
) {
}
