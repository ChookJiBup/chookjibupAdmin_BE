package com.example.demoadmin.map.query.application.dto;

import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import java.util.UUID;

/**
 * 축제 배치도 조회 결과를 표현한다.
 */
public record FestivalMapView(
        UUID mapId,
        FestivalMapStatus status,
        int width,
        int height
) {
}
