package com.example.chookjibupadmin.festival.query.application.dto;

import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DB projection 결과에 날짜 기준 진행 상태를 더하기 전의 축제 요약이다.
 */
public record InternalFestivalSummaryProjection(
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
        LocalTime operationEndTime
) {

    public InternalFestivalSummaryView toView(LocalDate today) {
        return new InternalFestivalSummaryView(
                festivalId,
                seriesId,
                name,
                description,
                address,
                detailAddress,
                year,
                startDate,
                endDate,
                operationStartTime,
                operationEndTime,
                FestivalProgressStatus.from(today, startDate, endDate)
        );
    }
}
