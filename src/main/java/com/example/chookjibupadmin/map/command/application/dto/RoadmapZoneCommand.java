package com.example.chookjibupadmin.map.command.application.dto;

import java.util.List;
import java.util.UUID;

public record RoadmapZoneCommand(
        UUID zoneId,
        String name,
        Integer sortOrder,
        List<UUID> boothNodeIds
) {
}
