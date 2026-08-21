package com.example.chookjibupadmin.visitor.query.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 축제 방문 인원 입력 현황 조회 결과이다.
 */
public record FestivalVisitorCountView(
        UUID festivalId,
        LocalDate startDate,
        LocalDate endDate,
        List<FestivalVisitorDayView> days,
        int filledDayCount,
        int totalDayCount,
        boolean allDaysFilled,
        int sumVisitorCount,
        Integer totalOverrideVisitorCount
) {
}
