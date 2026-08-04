package com.example.chookjibupadmin.map.analysis.domain;

import java.util.Optional;
import java.util.UUID;

public interface MapAnalysisJobRepository {
    MapAnalysisJob save(MapAnalysisJob job);
    Optional<MapAnalysisJob> findFirstPending();
    Optional<MapAnalysisJob> findByPublicId(UUID publicId);
    Optional<MapAnalysisJob> findLatestByMapId(Long mapId);
    void cancelActiveByMapId(Long mapId);
}
