package com.example.chookjibupadmin.festival.query.application.dto;

import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 사용자 서버에 전달할 축제 요약 조회 projection이다.
 */
public record InternalFestivalSummaryView(
        UUID festivalId,
        UUID seriesId,
        String name,
        String description,
        String address,
        String detailAddress,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime operationStartTime,
        LocalTime operationEndTime,
        FestivalProgressStatus progressStatus
) {
}
