package com.example.chookjibupadmin.map.query.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RoadmapNodeView(
        UUID nodeId,
        String nodeType,
        String name,
        String geometryType,
        Map<String, Object> geometry,
        BigDecimal confidence,
        String recognizedText,
        String source,
        String reviewStatus,
        int sortOrder,
        String geometrySchemaVersion
) {
}
