package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.visitor.command.application.dto.FestivalDailyVisitorCountResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

public record FestivalDailyVisitorCountResponse(
        @Schema(description = "축제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID festivalId,
        @Schema(description = "방문 일자", example = "2026-10-16")
        LocalDate visitDate,
        @Schema(description = "방문 인원 수", example = "12000")
        int visitorCount
) {

    public static FestivalDailyVisitorCountResponse from(
            FestivalDailyVisitorCountResult result
    ) {
        return new FestivalDailyVisitorCountResponse(
                result.festivalId(),
                result.visitDate(),
                result.visitorCount()
        );
    }
}
