package com.example.chookjibupadmin.map.query.application.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MapPresentationView(
        MapBoundaryView boundary,
        MapOverlayView overlay
) {

    public record MapBoundaryView(
            String geometryType,
            String schemaVersion,
            List<Map<String, BigDecimal>> points
    ) {
    }

    public record MapOverlayView(
            UUID assetId,
            URI imageUrl,
            Instant imageUrlExpiresAt,
            Integer imageWidth,
            Integer imageHeight,
            MapOverlayAnchorView anchor,
            MapOverlayCornersView corners,
            BigDecimal opacity,
            boolean visible,
            boolean clipToBoundary
    ) {
    }

    public record MapOverlayAnchorView(
            BigDecimal centerLatitude,
            BigDecimal centerLongitude,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees
    ) {
    }

    public record MapOverlayCornersView(
            MapLatLngView topLeft,
            MapLatLngView topRight,
            MapLatLngView bottomRight,
            MapLatLngView bottomLeft
    ) {
    }

    public record MapLatLngView(BigDecimal lat, BigDecimal lng) {
    }
}
