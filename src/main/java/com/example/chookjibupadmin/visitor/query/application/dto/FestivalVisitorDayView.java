package com.example.chookjibupadmin.visitor.query.application.dto;

import java.time.LocalDate;

/**
 * 축제 방문 인원 일자의 조회 결과이다.
 */
public record FestivalVisitorDayView(
        LocalDate visitDate,
        int dayIndex,
        Integer visitorCount,
        boolean inputAllowed,
        boolean saved
) {
}
