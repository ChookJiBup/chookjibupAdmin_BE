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
        int visitorCount,
        @Schema(description = "전 일자 입력 완료 여부", example = "false")
        boolean allDaysFilled,
        @Schema(description = "결과리포트 생성 가능 여부", example = "false")
        boolean reportReadyToGenerate
) {

    public static FestivalDailyVisitorCountResponse from(
            FestivalDailyVisitorCountResult result
    ) {
        return new FestivalDailyVisitorCountResponse(
                result.festivalId(),
                result.visitDate(),
                result.visitorCount(),
                result.allDaysFilled(),
                result.reportReadyToGenerate()
        );
    }
}
