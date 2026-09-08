package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.query.application.dto.MapImageAnchorView;
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
        List<ZoneResponse> zones,
        Center center,
        ImageAnchor imageAnchor
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
                view.zones().stream().map(zone -> new ZoneResponse(
                        zone.zoneId(), zone.name(), zone.sortOrder(), zone.boothNodeIds())).toList(),
                view.center() == null
                        ? null
                        : new Center(view.center().lat(), view.center().lng()),
                ImageAnchor.from(view.imageAnchor())
        );
    }

    public record Center(
            BigDecimal lat,
            BigDecimal lng
    ) {
    }

    /**
     * 배치도 이미지를 지도 위에 얹을 기준값. 좌표 전용 지도이거나 앵커가 아직 없으면 null이다.
     */
    public record ImageAnchor(
            BigDecimal centerLat,
            BigDecimal centerLng,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees
    ) {

        static ImageAnchor from(MapImageAnchorView view) {
            if (view == null) {
                return null;
            }
            return new ImageAnchor(
                    view.centerLat(),
                    view.centerLng(),
                    view.groundWidthMeters(),
                    view.rotationDegrees()
            );
        }
    }

    public record ZoneResponse(
            UUID zoneId,
            String name,
            int sortOrder,
            List<UUID> boothNodeIds
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
