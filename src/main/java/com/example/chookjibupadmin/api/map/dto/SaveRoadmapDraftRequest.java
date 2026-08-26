package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapZoneCommand;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SaveRoadmapDraftRequest(
        @NotNull @PositiveOrZero Long baseRevision,
        @NotNull @Size(min = 1, max = 1000)
        List<@Valid NodeChangeRequest> nodes,
        @Size(max = 200) List<@Valid ZoneRequest> zones
) {

    public SaveRoadmapDraftRequest(Long baseRevision, List<NodeChangeRequest> nodes) {
        this(baseRevision, nodes, null);
    }

    public SaveRoadmapDraftCommand toCommand(ObjectMapper objectMapper) {
        return new SaveRoadmapDraftCommand(
                baseRevision,
                nodes.stream()
                        .map(node -> node.toCommand(objectMapper))
                        .toList(),
                zones == null ? null : zones.stream().map(ZoneRequest::toCommand).toList()
        );
    }

    public record NodeChangeRequest(
            UUID nodeId,
            UUID clientNodeId,
            NodeType nodeType,
            @Size(max = 150) String name,
            GeometryType geometryType,
            Map<String, Object> geometry,
            @NotNull Boolean deleted,
            @PositiveOrZero Integer sortOrder
    ) {

        public NodeChangeRequest(
                UUID nodeId,
                NodeType nodeType,
                String name,
                GeometryType geometryType,
                Map<String, Object> geometry,
                Boolean deleted,
                Integer sortOrder
        ) {
            this(nodeId, null, nodeType, name, geometryType, geometry, deleted, sortOrder);
        }

        RoadmapNodeChangeCommand toCommand(ObjectMapper objectMapper) {
            return new RoadmapNodeChangeCommand(
                    nodeId,
                    clientNodeId,
                    nodeType,
                    name,
                    geometryType,
                    objectMapper.valueToTree(geometry),
                    Boolean.TRUE.equals(deleted),
                    sortOrder
            );
        }
    }

    public record ZoneRequest(
            UUID zoneId,
            @Size(min = 1, max = 100) String name,
            @NotNull @PositiveOrZero Integer sortOrder,
            @NotNull @Size(min = 1, max = 1000) List<UUID> boothNodeIds
    ) {
        RoadmapZoneCommand toCommand() {
            return new RoadmapZoneCommand(zoneId, name, sortOrder, boothNodeIds);
        }
    }
}
