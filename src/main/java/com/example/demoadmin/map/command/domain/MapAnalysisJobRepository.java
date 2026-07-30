package com.example.demoadmin.map.command.domain;

import java.util.Optional;
import java.util.UUID;

public interface MapAnalysisJobRepository {

    MapAnalysisJob save(MapAnalysisJob analysisJob);

    Optional<MapAnalysisJob> findById(Long analysisJobId);

    Optional<MapAnalysisJob> findByPublicId(UUID publicId);
}
