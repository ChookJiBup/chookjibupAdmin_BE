package com.example.chookjibupadmin.report.query.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 축제 종료 보고서 집계 지표를 제공하는 외부 데이터 계약이다.
 */
public interface FestivalReportMetricProvider {

    /**
     * 보고서 집계 지표를 조회하며 연동 데이터가 없으면 빈 값을 반환한다.
     */
    Optional<Snapshot> findSummary(Long festivalId);

    record Snapshot(
            long totalVisitorCount,
            long peakConcurrentVisitorCount,
            long averageWaitMinutes,
            LocalDateTime generatedAt
    ) {
    }
}
