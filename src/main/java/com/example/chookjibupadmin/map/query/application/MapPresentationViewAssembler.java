package com.example.chookjibupadmin.map.query.application;

import com.example.chookjibupadmin.map.analysis.application.MapAnchorProjector;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView.MapBoundaryView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView.MapLatLngView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView.MapOverlayAnchorView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView.MapOverlayCornersView;
import com.example.chookjibupadmin.map.query.application.dto.MapPresentationView.MapOverlayView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 표시 설정 Entity를 조회 View로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class MapPresentationViewAssembler {

    private static final TypeReference<Map<String, Object>> BOUNDARY_TYPE =
            new TypeReference<>() {
            };

    private final MapImageStoragePort storagePort;
    private final MapAnchorProjector mapAnchorProjector;
    private final ObjectMapper objectMapper;

    public MapPresentationView toView(FestivalMapPresentation presentation) {
        if (presentation == null) {
            return null;
        }
        return new MapPresentationView(
                toBoundary(presentation.getBoundaryGeometry()),
                toOverlay(presentation)
        );
    }

    private MapBoundaryView toBoundary(String boundaryGeometry) {
        if (boundaryGeometry == null || boundaryGeometry.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(boundaryGeometry, BOUNDARY_TYPE);
            Object geometryType = raw.get("geometryType");
            Object schemaVersion = raw.get("schemaVersion");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawPoints = (List<Map<String, Object>>) raw.get("points");
            if (!(geometryType instanceof String type)
                    || !(schemaVersion instanceof String version)
                    || rawPoints == null) {
                return null;
            }
            List<Map<String, BigDecimal>> points = rawPoints.stream()
                    .map(point -> Map.of(
                            "lat", toBigDecimal(point.get("lat")),
                            "lng", toBigDecimal(point.get("lng"))
                    ))
                    .toList();
            return new MapBoundaryView(type, version, points);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored map boundary geometry is invalid", exception);
        }
    }

    private MapOverlayView toOverlay(FestivalMapPresentation presentation) {
        if (!presentation.hasOverlayImage()) {
            return null;
        }
        MapImageReadUrl readUrl = storagePort.createReadUrl(
                presentation.getOverlayImageKey().getValue()
        );
        MapImageAnchor anchor = presentation.getOverlayImageAnchor();
        MapOverlayAnchorView anchorView = anchor == null
                ? null
                : new MapOverlayAnchorView(
                        anchor.getCenterLatitude(),
                        anchor.getCenterLongitude(),
                        anchor.getGroundWidthMeters(),
                        anchor.getRotationDegrees()
                );
        MapOverlayCornersView corners = null;
        if (anchor != null
                && presentation.getOverlayImageWidth() != null
                && presentation.getOverlayImageHeight() != null
                && presentation.getOverlayImageWidth() > 0
                && presentation.getOverlayImageHeight() > 0) {
            MapAnchorProjector.ProjectedCorners projected = mapAnchorProjector.corners(
                    anchor,
                    presentation.getOverlayImageWidth(),
                    presentation.getOverlayImageHeight()
            );
            corners = new MapOverlayCornersView(
                    new MapLatLngView(projected.topLeft().lat(), projected.topLeft().lng()),
                    new MapLatLngView(projected.topRight().lat(), projected.topRight().lng()),
                    new MapLatLngView(projected.bottomRight().lat(), projected.bottomRight().lng()),
                    new MapLatLngView(projected.bottomLeft().lat(), projected.bottomLeft().lng())
            );
        }
        return new MapOverlayView(
                presentation.getOverlayAssetId(),
                readUrl.url(),
                readUrl.expiresAt(),
                presentation.getOverlayImageWidth(),
                presentation.getOverlayImageHeight(),
                anchorView,
                corners,
                presentation.getOverlayOpacity(),
                presentation.isOverlayVisible(),
                presentation.isClipToBoundary()
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new IllegalStateException("Boundary point is not numeric");
    }
}
