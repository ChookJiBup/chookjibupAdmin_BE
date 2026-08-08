package com.example.chookjibupadmin.dashboard.query.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FestivalDashboardView(
        UUID festivalId,
        boolean dataAvailable,
        String operatingStatus,
        long currentVisitorCount,
        long activeQueueCount,
        long averageWaitMinutes,
        LocalDateTime updatedAt
) {
}
