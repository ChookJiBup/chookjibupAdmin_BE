package com.example.chookjibupadmin.api.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 배치도 이미지를 지도 위에 얹을 기준값 수정 요청이다.
 *
 * <p>값의 범위 검증은 {@code MapImageAnchor.of(...)}가 담당하므로 여기서는 누락만 막는다.</p>
 */
@Schema(description = "축제 배치도 이미지 앵커 수정 요청")
public record UpdateMapImageAnchorRequest(
        @NotNull
        @Schema(description = "이미지 중심이 놓이는 위도", example = "37.5665")
        BigDecimal centerLat,

        @NotNull
        @Schema(description = "이미지 중심이 놓이는 경도", example = "126.9780")
        BigDecimal centerLng,

        @NotNull
        @Schema(description = "이미지 가로폭이 덮는 실거리(m)", example = "300.00")
        BigDecimal groundWidthMeters,

        @NotNull
        @Schema(description = "이미지 위쪽이 가리키는 방위각(북=0, 시계방향)", example = "0.000")
        BigDecimal rotationDegrees
) {
}
