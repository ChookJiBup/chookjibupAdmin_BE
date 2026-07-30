package com.example.demoadmin.map.command.application.port;

import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObjectType;

public record DetectedMapObject(
        MapObjectType type,
        String name,
        GeometryType geometryType,
        String geometryData,
        double confidence
) {
}
