package com.example.demoadmin.api.map.dto;

import com.example.demoadmin.map.command.domain.MapDetectionSource;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapReviewStatus;
import com.example.demoadmin.map.query.application.dto.MapObjectView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "React-Konva에서 사용할 배치도 객체")
public record MapObjectResponse(
        UUID objectId,
        MapObjectType type,
        String name,
        Map<String, Object> geometry,
        double confidence,
        MapReviewStatus reviewStatus,
        MapDetectionSource source
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static MapObjectResponse from(MapObjectView view) {
        try {
            return new MapObjectResponse(
                    view.objectId(),
                    view.type(),
                    view.name(),
                    OBJECT_MAPPER.readValue(
                            view.geometryData(),
                            new TypeReference<>() {
                            }
                    ),
                    view.confidence(),
                    view.reviewStatus(),
                    view.source()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "저장된 배치도 geometry를 해석할 수 없습니다.",
                    exception
            );
        }
    }
}
