package com.example.demoadmin.api.internal.festival.dto;

import com.example.demoadmin.festival.query.application.dto.InternalFestivalProgressStatus;
import com.example.demoadmin.festival.query.application.dto.InternalFestivalSummaryView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 사용자 서버용 축제 요약 응답이다.
 */
public record InternalFestivalSummaryResponse(
        UUID festivalId,
        UUID seriesId,
        String name,
        String description,
        String address,
        String detailAddress,
        int year,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime operationStartTime,
        LocalTime operationEndTime,
        InternalFestivalProgressStatus progressStatus
) {

    public static InternalFestivalSummaryResponse from(
            InternalFestivalSummaryView view
    ) {
        return new InternalFestivalSummaryResponse(
                view.festivalId(),
                view.seriesId(),
                view.name(),
                view.description(),
                view.address(),
                view.detailAddress(),
                view.year(),
                view.startDate(),
                view.endDate(),
                view.operationStartTime(),
                view.operationEndTime(),
                view.progressStatus()
        );
    }
}
