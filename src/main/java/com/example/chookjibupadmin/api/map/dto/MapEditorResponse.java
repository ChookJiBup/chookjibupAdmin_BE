package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.query.application.dto.RoadmapNodeView;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MapEditorResponse(
        UUID mapId,
        URI displayImageUrl,
        Instant displayImageUrlExpiresAt,
        int imageWidth,
        int imageHeight,
        long editRevision,
        String roadmapStatus,
        MapAnalysisStatusResponse analysis,
        List<NodeResponse> nodes
) {

    public static MapEditorResponse from(MapEditorView view) {
        return new MapEditorResponse(
                view.mapId(),
                view.displayImageUrl(),
                view.displayImageUrlExpiresAt(),
                view.imageWidth(),
                view.imageHeight(),
                view.editRevision(),
                view.roadmapStatus(),
                MapAnalysisStatusResponse.from(view.analysis()),
                view.nodes().stream()
                        .map(NodeResponse::from)
                        .toList()
        );
    }

    public record NodeResponse(
            UUID nodeId,
            String nodeType,
            String name,
            String geometryType,
            Map<String, Object> geometry,
            BigDecimal confidence,
            String recognizedText,
            String source,
            String reviewStatus,
            int sortOrder
    ) {

        static NodeResponse from(RoadmapNodeView view) {
            return new NodeResponse(
                    view.nodeId(),
                    view.nodeType(),
                    view.name(),
                    view.geometryType(),
                    view.geometry(),
                    view.confidence(),
                    view.recognizedText(),
                    view.source(),
                    view.reviewStatus(),
                    view.sortOrder()
            );
        }
    }
}
