package com.example.chookjibupadmin.dashboard.query.infrastructure;

import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import java.util.Optional;

public class UnavailableFestivalDashboardMetricProvider
        implements FestivalDashboardMetricProvider {

    @Override
    public Optional<Snapshot> findCurrent(Long festivalId) {
        return Optional.empty();
    }
}
