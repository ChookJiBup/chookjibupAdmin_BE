package com.example.chookjibupadmin.report.query.infrastructure;

import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import java.util.Optional;

public class UnavailableFestivalReportMetricProvider
        implements FestivalReportMetricProvider {

    @Override
    public Optional<Snapshot> findSummary(Long festivalId) {
        return Optional.empty();
    }
}
