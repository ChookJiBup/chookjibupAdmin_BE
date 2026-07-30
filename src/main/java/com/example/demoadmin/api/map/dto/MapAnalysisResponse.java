package com.example.demoadmin.api.map.dto;

import com.example.demoadmin.map.command.application.dto.MapAnalysisResultView;
import com.example.demoadmin.map.command.domain.MapAnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "배치도 분석 완료 응답")
public record MapAnalysisResponse(
        @Schema(description = "배치도 외부 ID")
        UUID mapId,
        @Schema(description = "분석 작업 외부 ID")
        UUID analysisJobId,
        @Schema(description = "분석 작업 상태")
        MapAnalysisStatus status,
        @Schema(description = "탐지된 객체 수")
        int objectCount
) {

    public static MapAnalysisResponse from(MapAnalysisResultView result) {
        return new MapAnalysisResponse(
                result.mapId(),
                result.analysisJobId(),
                result.status(),
                result.objectCount()
        );
    }
}
