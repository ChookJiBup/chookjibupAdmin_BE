package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapZoneCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.BoundaryGeometryCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.LatLngPointCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveMapPresentationCommand.OverlayPresentationCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SaveRoadmapDraftRequest(
        @NotNull @PositiveOrZero Long baseRevision,
        @NotNull @Size(max = 1000)
        List<@Valid NodeChangeRequest> nodes,
        @Size(max = 200) List<@Valid ZoneRequest> zones,
        @Valid PresentationRequest presentation
) {

    public SaveRoadmapDraftRequest(Long baseRevision, List<NodeChangeRequest> nodes) {
        this(baseRevision, nodes, null, null);
    }

    public SaveRoadmapDraftRequest(
            Long baseRevision,
            List<NodeChangeRequest> nodes,
            List<ZoneRequest> zones
    ) {
        this(baseRevision, nodes, zones, null);
    }

    /**
     * 노드 목록은 비어 있어도 되지만(경계·팜플렛만 저장), 노드도 표시 설정도 없으면
     * 저장할 것이 없다.
     */
    @AssertTrue(message = "저장할 노드나 표시 설정이 필요합니다.")
    public boolean isSomethingToSave() {
        return nodes == null || !nodes.isEmpty() || presentation != null;
    }

    public SaveRoadmapDraftCommand toCommand(ObjectMapper objectMapper) {
        return new SaveRoadmapDraftCommand(
                baseRevision,
                nodes.stream()
                        .map(node -> node.toCommand(objectMapper))
                        .toList(),
                zones == null ? null : zones.stream().map(ZoneRequest::toCommand).toList(),
                presentation == null ? null : presentation.toCommand()
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

    /**
     * 지도 표시 설정 부분 수정. 오버레이 이미지는 별도 업로드 API의 assetId를 참조한다.
     */
    public record PresentationRequest(
            Boolean clearBoundary,
            @Valid BoundaryRequest boundary,
            Boolean clearOverlay,
            @Valid OverlayRequest overlay
    ) {
        SaveMapPresentationCommand toCommand() {
            return new SaveMapPresentationCommand(
                    clearBoundary,
                    boundary == null ? null : boundary.toCommand(),
                    clearOverlay,
                    overlay == null ? null : overlay.toCommand()
            );
        }
    }

    public record BoundaryRequest(
            String geometryType,
            String schemaVersion,
            @Size(min = 3, max = 500) List<@Valid BoundaryPointRequest> points
    ) {
        BoundaryGeometryCommand toCommand() {
            return new BoundaryGeometryCommand(
                    geometryType,
                    schemaVersion,
                    points == null
                            ? null
                            : points.stream()
                                    .map(point -> new LatLngPointCommand(point.lat(), point.lng()))
                                    .toList()
            );
        }
    }

    public record BoundaryPointRequest(
            @NotNull @DecimalMin("33.0") @DecimalMax("38.7") BigDecimal lat,
            @NotNull @DecimalMin("124.5") @DecimalMax("132.0") BigDecimal lng
    ) {
    }

    public record OverlayRequest(
            UUID assetId,
            BigDecimal centerLatitude,
            BigDecimal centerLongitude,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal opacity,
            Boolean visible,
            Boolean clipToBoundary
    ) {
        OverlayPresentationCommand toCommand() {
            return new OverlayPresentationCommand(
                    assetId,
                    centerLatitude,
                    centerLongitude,
                    groundWidthMeters,
                    rotationDegrees,
                    opacity,
                    visible,
                    clipToBoundary
            );
        }
    }
}
