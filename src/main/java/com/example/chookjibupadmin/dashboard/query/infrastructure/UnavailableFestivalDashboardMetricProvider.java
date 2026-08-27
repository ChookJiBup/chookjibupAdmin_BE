package com.example.chookjibupadmin.dashboard.query.infrastructure;

import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import java.util.Optional;

/**
 * 실지표 Bean이 없을 때 사용하는 미연결 fallback이다.
 */
public class UnavailableFestivalDashboardMetricProvider
        implements FestivalDashboardMetricProvider {

    @Override
    public Optional<Snapshot> findCurrent(Long festivalId) {
        return Optional.empty();
    }
}
