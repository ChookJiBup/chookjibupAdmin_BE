package com.example.demoadmin.map.query.application.dto;

import java.util.List;

/**
 * 배치도 정보와 해당 배치도 객체 목록을 함께 반환한다.
 */
public record FestivalMapObjectsView(
        FestivalMapView map,
        List<MapObjectView> objects
) {

    public FestivalMapObjectsView {
        objects = List.copyOf(objects);
    }
}
