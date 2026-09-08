package com.example.chookjibupadmin.map.query.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FestivalOperationsMapView(
        UUID mapId,
        long editRevision,
        String mapKind,
        MapPresentationView presentation,
        List<ApprovedBoothMarkerView> booths
) {

    public record ApprovedBoothMarkerView(
            Long boothId,
            UUID nodeId,
            String name,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
