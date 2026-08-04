package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "축제 등록 시 첨부한 AI 분석용 도면 이미지 응답")
public record CreateFestivalMapResponse(
        @Schema(description = "배치도 UUID")
        UUID mapId,
        @Schema(description = "배치도 이름")
        String mapName,
        @Schema(description = "파일 저장 상태")
        String storageStatus,
        @Schema(description = "화면 표시용 도면 이미지 너비")
        int imageWidth,
        @Schema(description = "화면 표시용 도면 이미지 높이")
        int imageHeight,
        @Schema(description = "분석 작업 UUID") UUID analysisJobId,
        @Schema(description = "분석 상태") String analysisStatus
) {

    public static CreateFestivalMapResponse from(FestivalMap festivalMap, MapAnalysisJob job) {
        return new CreateFestivalMapResponse(
                festivalMap.getPublicId(),
                festivalMap.getMapName().getValue(),
                festivalMap.getStorageStatus().name(),
                festivalMap.getDisplayImageDimensions().getWidth(),
                festivalMap.getDisplayImageDimensions().getHeight(),
                job == null ? null : job.getPublicId(),
                job == null ? null : job.getStatus().name()
        );
    }

    public static CreateFestivalMapResponse from(FestivalMap festivalMap) {
        return from(festivalMap, null);
    }
}
