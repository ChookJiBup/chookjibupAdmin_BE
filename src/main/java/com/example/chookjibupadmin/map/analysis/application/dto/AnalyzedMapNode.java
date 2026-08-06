package com.example.chookjibupadmin.map.analysis.application.dto;

import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record AnalyzedMapNode(
        NodeType nodeType,
        String name,
        GeometryType geometryType,
        JsonNode geometry,
        BigDecimal confidence,
        String recognizedText
) {
}
