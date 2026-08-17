package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.query.application.dto.RoadmapNodeView;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MapEditorResponse(
        UUID mapId,
        URI displayImageUrl,
        Instant displayImageUrlExpiresAt,
        Integer imageWidth,
        Integer imageHeight,
        long editRevision,
        String roadmapStatus,
        MapAnalysisStatusResponse analysis,
        List<NodeResponse> nodes,
        Center center
) {

    public static MapEditorResponse from(MapEditorView view) {
        return new MapEditorResponse(
                view.mapId(),
                view.displayImageUrl(),
                view.displayImageUrlExpiresAt(),
                view.imageWidth() > 0 ? view.imageWidth() : null,
                view.imageHeight() > 0 ? view.imageHeight() : null,
                view.editRevision(),
                view.roadmapStatus(),
                view.analysis() == null
                        ? null
                        : MapAnalysisStatusResponse.from(view.analysis()),
                view.nodes().stream()
                        .map(NodeResponse::from)
                        .toList(),
                view.center() == null
                        ? null
                        : new Center(view.center().lat(), view.center().lng())
        );
    }

    public record Center(
            BigDecimal lat,
            BigDecimal lng
    ) {
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
            int sortOrder,
            String geometrySchemaVersion
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
                    view.sortOrder(),
                    view.geometrySchemaVersion()
            );
        }
    }
}
