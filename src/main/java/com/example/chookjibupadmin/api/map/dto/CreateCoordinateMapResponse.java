package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.CoordinateMapView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "좌표 전용 축제 지도 요약")
public record CreateCoordinateMapResponse(
        UUID mapId,
        String mapName,
        long editRevision,
        String roadmapStatus,
        Center center
) {

    public static CreateCoordinateMapResponse from(CoordinateMapView view) {
        return new CreateCoordinateMapResponse(
                view.mapId(),
                view.mapName(),
                view.editRevision(),
                view.roadmapStatus(),
                new Center(view.center().lat(), view.center().lng())
        );
    }

    @Schema(description = "카카오맵 초기 중심 좌표")
    public record Center(
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
