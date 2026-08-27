package com.example.chookjibupadmin.dashboard.query.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
            List<BoothMetric> booths
    ) {
    }

    record BoothMetric(
            Long boothId,
            String boothName,
            String congestionLevel,
            Integer waitMinutes,
            LocalDateTime congestionCreatedAt
    ) {
    }
}
