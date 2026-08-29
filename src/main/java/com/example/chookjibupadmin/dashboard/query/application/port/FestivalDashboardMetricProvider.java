package com.example.chookjibupadmin.dashboard.query.application.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 축제의 현재 운영 지표를 제공하는 계약이다.
 */
public interface FestivalDashboardMetricProvider {

    Optional<Snapshot> findCurrent(Long festivalId);

    record Snapshot(
            boolean visitorAvailable,
            boolean boothAvailable,
            boolean congestionAvailable,
            boolean summaryAvailable,
            String operatingStatus,
            Long currentVisitorCount,
            Long activeQueueCount,
            Long averageWaitMinutes,
            LocalDateTime updatedAt,
            List<BoothMetric> booths,
            List<ZoneMetric> zones
    ) {
    }

    record BoothMetric(
            Long boothId,
            String boothName,
            UUID roadmapNodePublicId,
            BigDecimal lat,
            BigDecimal lng,
            String congestionLevel,
            Integer waitMinutes,
            LocalDateTime congestionCreatedAt,
            String modifierType,
            Long modifierAdminId,
            Long modifierStaffId,
            String modifierName
    ) {
    }

    record ZoneMetric(
            UUID zoneId,
            String name,
            int sortOrder,
            List<UUID> boothNodeIds
    ) {
    }
}
