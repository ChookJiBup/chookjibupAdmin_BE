package com.example.chookjibupadmin.map.command.application.dto;

import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record RoadmapNodeChangeCommand(
        UUID nodeId,
        UUID clientNodeId,
        NodeType nodeType,
        String name,
        GeometryType geometryType,
        JsonNode geometry,
        boolean deleted,
        Integer sortOrder
) {
    public RoadmapNodeChangeCommand(
            UUID nodeId,
            NodeType nodeType,
            String name,
            GeometryType geometryType,
            JsonNode geometry,
            boolean deleted,
            Integer sortOrder
    ) {
        this(nodeId, null, nodeType, name, geometryType, geometry, deleted, sortOrder);
    }
}
