package com.example.demoadmin.map.query.application.dto;

import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapDetectionSource;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapReviewStatus;
import java.util.UUID;

/**
 * 배치도 객체 조회 결과를 표현한다.
 */
public record MapObjectView(
        UUID objectId,
        MapObjectType type,
        String name,
        GeometryType geometryType,
        String geometryData,
        double confidence,
        MapReviewStatus reviewStatus,
        MapDetectionSource source
) {
}
