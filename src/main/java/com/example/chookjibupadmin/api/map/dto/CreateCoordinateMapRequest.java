package com.example.chookjibupadmin.api.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "좌표 전용 축제 지도 생성 요청")
public record CreateCoordinateMapRequest(
        @NotBlank @Size(max = 150)
        @Schema(description = "지도 버전 이름", example = "본행사 배치")
        String mapName
) {
}
