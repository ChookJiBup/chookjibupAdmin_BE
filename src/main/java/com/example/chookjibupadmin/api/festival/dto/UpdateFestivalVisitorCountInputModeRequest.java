package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 축제 방문 인원 집계 방식 변경 요청 DTO이다.
 */
@Schema(description = "축제 방문 인원 집계 방식 변경 요청")
public record UpdateFestivalVisitorCountInputModeRequest(
        @Schema(
                description = "방문 인원 집계 방식",
                example = "DAILY",
                allowableValues = {"DAILY", "TOTAL"}
        )
        @NotNull
        FestivalVisitorCountInputMode visitorCountInputMode
) {
}
