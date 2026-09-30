package com.example.chookjibupadmin.festival.query.application.dto;

import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 저장된 진행 상태를 포함하는 축제 요약이다.
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
        LocalTime operationEndTime,
        String progressStatus
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
                FestivalProgressStatus.resolve(progressStatus, today, startDate, endDate)
        );
    }
}
