package com.example.chookjibupadmin.festival.query.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 축제 등록 시 기존 축제 시리즈와 최근 개최 정보를 제공한다.
 */
public record FestivalSeriesSearchView(
        UUID seriesId,
        String name,
        UUID latestFestivalId,
        Integer latestYear,
        String latestDescription,
        String latestAddress,
        String latestDetailAddress,
        LocalDate latestStartDate,
        LocalDate latestEndDate,
        LocalTime latestOperationStartTime,
        LocalTime latestOperationEndTime
) {
}
