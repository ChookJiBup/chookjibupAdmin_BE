package com.example.demoadmin.api.map.dto;

import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import com.example.demoadmin.map.query.application.dto.FestivalMapObjectsView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "배치도와 React-Konva용 객체 목록")
public record FestivalMapObjectsResponse(
        UUID mapId,
        FestivalMapStatus status,
        int imageWidth,
        int imageHeight,
        List<MapObjectResponse> objects
) {

    public static FestivalMapObjectsResponse from(FestivalMapObjectsView view) {
        return new FestivalMapObjectsResponse(
                view.map().mapId(),
                view.map().status(),
                view.map().width(),
                view.map().height(),
                view.objects().stream()
                        .map(MapObjectResponse::from)
                        .toList()
        );
    }
}
