package com.example.chookjibupadmin.map.query.application.dto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record MapEditorView(
        UUID mapId,
        URI displayImageUrl,
        Instant displayImageUrlExpiresAt,
        int imageWidth,
        int imageHeight,
        long editRevision,
        String roadmapStatus,
        MapAnalysisStatusView analysis,
        List<RoadmapNodeView> nodes
) {
}
