package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "축제 기본 정보 생성 응답")
public record CreateFestivalResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,
        @Schema(description = "같은 축제를 연도별로 묶는 축제 묶음 UUID", example = "22222222-2222-2222-2222-222222222222")
        UUID seriesId,
        @Schema(description = "개최 연도", example = "2026")
        int year,
        @Schema(description = "축제명", example = "마포나루 새우젓축제")
        String name,
        @Schema(description = "축제 시작일", example = "2026-10-16")
        LocalDate startDate,
        @Schema(description = "축제 종료일", example = "2026-10-18")
        LocalDate endDate,
        @Schema(description = "축제 상태", example = "DRAFT")
        String status,
        @Schema(description = "운영 시작 시간", example = "10:00:00")
        LocalTime operationStartTime,
        @Schema(description = "운영 종료 시간", example = "21:00:00")
        LocalTime operationEndTime
) {

    /**
     * 저장된 축제 Aggregate를 API 응답으로 변환한다.
     */
    public static CreateFestivalResponse from(Festival festival) {
        return new CreateFestivalResponse(
                festival.getPublicId(),
                festival.getSeriesPublicId(),
                festival.getYear(),
                festival.getNameValue(),
                festival.getStartDate(),
                festival.getEndDate(),
                festival.getStatus().name(),
                festival.getOperationStartTime(),
                festival.getOperationEndTime()
        );
    }
}
