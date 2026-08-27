package com.example.chookjibupadmin.dashboard.query.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FestivalDashboardView(
        UUID festivalId,
        boolean dataAvailable,
        boolean visitorAvailable,
        boolean boothAvailable,
        boolean congestionAvailable,
        boolean summaryAvailable,
        String operatingStatus,
        Long currentVisitorCount,
        Long activeQueueCount,
        Long averageWaitMinutes,
        LocalDateTime updatedAt,
        List<DashboardBoothView> booths
) {
}
