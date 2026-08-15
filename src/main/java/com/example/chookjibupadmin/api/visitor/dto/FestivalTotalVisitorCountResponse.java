package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.visitor.command.application.dto.FestivalTotalVisitorCountResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record FestivalTotalVisitorCountResponse(
        @Schema(description = "축제 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID festivalId,
        @Schema(description = "총 방문 인원 수", example = "48000")
        int visitorCount
) {

    public static FestivalTotalVisitorCountResponse from(
            FestivalTotalVisitorCountResult result
    ) {
        return new FestivalTotalVisitorCountResponse(
                result.festivalId(),
                result.visitorCount()
        );
    }
}
