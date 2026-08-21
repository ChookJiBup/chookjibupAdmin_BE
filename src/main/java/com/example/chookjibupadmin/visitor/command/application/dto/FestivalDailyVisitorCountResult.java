package com.example.chookjibupadmin.visitor.command.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 일자별 방문 인원 수 저장 결과이다.
 */
public record FestivalDailyVisitorCountResult(
        UUID festivalId,
        LocalDate visitDate,
        int visitorCount,
        boolean allDaysFilled,
        boolean reportReadyToGenerate
) {
}
