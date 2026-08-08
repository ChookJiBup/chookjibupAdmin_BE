package com.example.chookjibupadmin.dashboard.query.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 축제의 현재 운영 지표를 제공하는 외부 데이터 계약이다.
 */
public interface FestivalDashboardMetricProvider {

    /**
     * 현재 운영 지표를 조회하며 연동 데이터가 없으면 빈 값을 반환한다.
     */
    Optional<Snapshot> findCurrent(Long festivalId);

    record Snapshot(
            String operatingStatus,
            long currentVisitorCount,
            long activeQueueCount,
            long averageWaitMinutes,
            LocalDateTime updatedAt
    ) {
    }
}
