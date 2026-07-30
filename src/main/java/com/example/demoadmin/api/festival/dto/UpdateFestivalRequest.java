package com.example.demoadmin.api.festival.dto;

import com.example.demoadmin.festival.command.application.dto.UpdateFestivalCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "축제 기본 정보 수정 요청")
public record UpdateFestivalRequest(
        @Schema(description = "축제명", example = "마포나루 새우젓축제")
        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @Schema(description = "축제 설명", example = "마포구 대표 지역 축제")
        @NotBlank
        @Size(max = 1000)
        String description,

        @Schema(description = "축제 주소", example = "서울특별시 마포구 월드컵로 243")
        @NotBlank
        @Size(min = 2, max = 255)
        String address,

        @Schema(description = "축제 상세주소", example = "광주비엔날레 전시관")
        @Size(max = 100)
        String detailAddress,

        @Schema(description = "축제 시작일", example = "2026-10-16")
        @NotNull
        LocalDate startDate,

        @Schema(description = "축제 종료일", example = "2026-10-18")
        @NotNull
        LocalDate endDate,

        @Schema(description = "운영 시작 시간", example = "10:00:00")
        @NotNull
        LocalTime operationStartTime,

        @Schema(description = "운영 종료 시간", example = "21:00:00")
        @NotNull
        LocalTime operationEndTime
) {

    /**
     * HTTP 요청을 축제 수정 Command로 변환한다.
     */
    public UpdateFestivalCommand toCommand() {
        return new UpdateFestivalCommand(
                name,
                description,
                address,
                detailAddress,
                startDate,
                endDate,
                operationStartTime,
                operationEndTime
        );
    }
}
