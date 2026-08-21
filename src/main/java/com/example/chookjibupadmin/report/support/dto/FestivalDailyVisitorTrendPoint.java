package com.example.chookjibupadmin.report.support.dto;

import java.time.LocalDate;

/**
 * 축제 일차 기준으로 정렬한 올해·전년 방문객 추이 한 지점이다.
 */
public record FestivalDailyVisitorTrendPoint(
        int dayIndex,
        LocalDate visitDate,
        Long currentCount,
        Long previousCount
) {
}
