package com.example.chookjibupadmin.map.query.application.dto;

import java.util.List;
import java.util.UUID;

public record RoadmapZoneView(
        UUID zoneId,
        String name,
        int sortOrder,
        List<UUID> boothNodeIds
) {
}
