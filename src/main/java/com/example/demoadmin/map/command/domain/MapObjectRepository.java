package com.example.demoadmin.map.command.domain;

import java.util.List;

public interface MapObjectRepository {

    MapObject save(MapObject mapObject);

    List<MapObject> saveAll(List<MapObject> mapObjects);

    List<MapObject> findByFestivalMapId(Long festivalMapId);
}
