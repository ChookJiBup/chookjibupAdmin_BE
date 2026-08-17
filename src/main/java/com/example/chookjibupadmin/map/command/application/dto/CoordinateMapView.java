package com.example.chookjibupadmin.map.command.application.dto;

import com.example.chookjibupadmin.map.query.application.dto.MapCenterView;
import java.util.UUID;

/** 좌표 전용 지도 생성·조회 결과이다. */
public record CoordinateMapView(
        UUID mapId,
        String mapName,
        long editRevision,
        String roadmapStatus,
        MapCenterView center
) {
}
