package com.example.chookjibupadmin.dashboard.query.application.dto;

import java.util.List;
import java.util.UUID;

public record DashboardZoneView(
        UUID zoneId,
        String name,
        int sortOrder,
        List<UUID> boothNodeIds
) {
}
