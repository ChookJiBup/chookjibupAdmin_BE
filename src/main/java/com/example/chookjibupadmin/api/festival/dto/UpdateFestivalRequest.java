package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

        @Schema(description = "축제 장소 목록")
        @NotEmpty
        @Size(max = 100)
        List<@Valid FestivalLocationRequest> locations,

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
    public UpdateFestivalRequest(
            String name,
            String description,
            String address,
            String detailAddress,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime operationStartTime,
            LocalTime operationEndTime
    ) {
        this(
                name,
                description,
                List.of(
                        new FestivalLocationRequest(
                                null,
                                FestivalLocationType.MAIN_VENUE,
                                "메인 행사장",
                                address,
                                null,
                                detailAddress,
                                null,
                                null,
                                null,
                                null,
                                null,
                                true,
                                0
                        )),
                startDate,
                endDate,
                operationStartTime,
                operationEndTime
        );
    }

    /**
     * HTTP 요청을 축제 수정 Command로 변환한다.
     */
    public UpdateFestivalCommand toCommand() {
        return new UpdateFestivalCommand(
                name,
                description,
                locations.stream().map(FestivalLocationRequest::toCommand).toList(),
                startDate,
                endDate,
                operationStartTime,
                operationEndTime
        );
    }

    public String address() {
        return primary().roadAddress() != null ? primary().roadAddress() : primary().jibunAddress();
    }

    public String detailAddress() {
        return primary().detailAddress();
    }

    private FestivalLocationRequest primary() {
        return locations.stream()
                .filter(FestivalLocationRequest::primary)
                .findFirst()
                .orElse(locations.getFirst());
    }
}
