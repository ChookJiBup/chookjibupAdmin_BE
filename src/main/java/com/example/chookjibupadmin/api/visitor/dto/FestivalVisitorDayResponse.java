package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.visitor.query.application.dto.FestivalVisitorDayView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "축제 방문 인원 일자 항목")
public record FestivalVisitorDayResponse(
        @Schema(description = "방문 일자", example = "2026-10-16")
        LocalDate visitDate,
        @Schema(description = "축제 일차(1부터)", example = "1")
        int dayIndex,
        @Schema(description = "저장된 방문 인원 수, 미입력 시 null", nullable = true)
        Integer visitorCount,
        @Schema(description = "일일마감 후 입력 가능 여부", example = "true")
        boolean inputAllowed,
        @Schema(description = "저장 여부", example = "false")
        boolean saved
) {

    public static FestivalVisitorDayResponse from(FestivalVisitorDayView view) {
        return new FestivalVisitorDayResponse(
                view.visitDate(),
                view.dayIndex(),
                view.visitorCount(),
                view.inputAllowed(),
                view.saved()
        );
    }
}
