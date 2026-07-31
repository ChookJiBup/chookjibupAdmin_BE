package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 축제 등록용 기존 축제 검색 응답 DTO이다.
 */
@Schema(description = "축제 등록용 기존 축제 검색 응답")
public record FestivalSeriesSearchResponse(
        @Schema(description = "외부 노출용 축제 시리즈 ID")
        UUID seriesId,
        @Schema(description = "축제명", example = "김밥축제")
        String name,
        @Schema(description = "가장 최근 개최 축제 ID")
        UUID latestFestivalId,
        @Schema(description = "가장 최근 개최 연도", example = "2025")
        Integer latestYear,
        @Schema(description = "가장 최근 축제 설명")
        String latestDescription,
        @Schema(description = "가장 최근 축제 주소")
        String latestAddress,
        @Schema(description = "가장 최근 축제 상세주소")
        String latestDetailAddress,
        @Schema(description = "가장 최근 축제 시작일")
        LocalDate latestStartDate,
        @Schema(description = "가장 최근 축제 종료일")
        LocalDate latestEndDate,
        @Schema(description = "가장 최근 축제 운영 시작 시각")
        LocalTime latestOperationStartTime,
        @Schema(description = "가장 최근 축제 운영 종료 시각")
        LocalTime latestOperationEndTime
) {

    /**
     * 축제 시리즈 검색 결과를 HTTP 응답 DTO로 변환한다.
     */
    public static FestivalSeriesSearchResponse from(
            FestivalSeriesSearchView view
    ) {
        return new FestivalSeriesSearchResponse(
                view.seriesId(),
                view.name(),
                view.latestFestivalId(),
                view.latestYear(),
                view.latestDescription(),
                view.latestAddress(),
                view.latestDetailAddress(),
                view.latestStartDate(),
                view.latestEndDate(),
                view.latestOperationStartTime(),
                view.latestOperationEndTime()
        );
    }
}
