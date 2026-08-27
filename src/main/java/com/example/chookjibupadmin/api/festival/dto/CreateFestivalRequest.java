package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "축제 기본 정보 생성 요청")
public record CreateFestivalRequest(
        @Schema(description = "기존 축제 묶음 UUID. 없으면 축제명 기준으로 자동 생성 또는 연결", example = "22222222-2222-2222-2222-222222222222")
        UUID seriesId,

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
        LocalTime operationEndTime,

        @Schema(description = "방문 인원 입력 모드. null이면 UNSET", example = "DAILY", allowableValues = {"DAILY", "TOTAL"})
        FestivalVisitorCountInputMode visitorCountInputMode
) {
    public CreateFestivalRequest(
            UUID seriesId,
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
                seriesId,
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
                operationEndTime,
                null
        );
    }

    /**
     * HTTP 요청을 축제 생성 Command로 변환한다.
     */
    public CreateFestivalCommand toCommand() {
        return new CreateFestivalCommand(
                seriesId,
                name,
                description,
                locations.stream().map(FestivalLocationRequest::toCommand).toList(),
                startDate,
                endDate,
                operationStartTime,
                operationEndTime,
                visitorCountInputMode
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
