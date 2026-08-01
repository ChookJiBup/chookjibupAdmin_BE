package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "축제 등록 시 첨부한 배치도 이미지 응답")
public record CreateFestivalMapResponse(
        @Schema(description = "배치도 UUID")
        UUID mapId,
        @Schema(description = "배치도 이름")
        String mapName,
        @Schema(description = "파일 저장 상태")
        String storageStatus,
        @Schema(description = "화면 표시용 이미지 너비")
        int imageWidth,
        @Schema(description = "화면 표시용 이미지 높이")
        int imageHeight
) {

    public static CreateFestivalMapResponse from(FestivalMap festivalMap) {
        return new CreateFestivalMapResponse(
                festivalMap.getPublicId(),
                festivalMap.getMapName(),
                festivalMap.getStorageStatus().name(),
                festivalMap.getImageWidth(),
                festivalMap.getImageHeight()
        );
    }
}
