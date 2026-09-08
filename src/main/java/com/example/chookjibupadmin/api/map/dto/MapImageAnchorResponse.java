package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 저장된 배치도 이미지 앵커를 응답한다. 값은 서버가 반올림한 뒤의 최종본이다.
 */
@Schema(description = "축제 배치도 이미지 앵커")
public record MapImageAnchorResponse(
        UUID mapId,
        BigDecimal centerLat,
        BigDecimal centerLng,
        BigDecimal groundWidthMeters,
        BigDecimal rotationDegrees
) {

    public static MapImageAnchorResponse of(UUID mapId, MapImageAnchor anchor) {
        return new MapImageAnchorResponse(
                mapId,
                anchor.getCenterLatitude(),
                anchor.getCenterLongitude(),
                anchor.getGroundWidthMeters(),
                anchor.getRotationDegrees()
        );
    }
}
