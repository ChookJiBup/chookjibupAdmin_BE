package com.example.chookjibupadmin.map.analysis.infrastructure.persistence;

import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobRepository;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MapAnalysisJobRepositoryImpl implements MapAnalysisJobRepository {

    private final MapAnalysisJobJpaRepository jpaRepository;

    @Override
    public MapAnalysisJob save(MapAnalysisJob job) {
        return jpaRepository.save(job);
    }

    @Override
    public Optional<MapAnalysisJob> findFirstPending() {
        return jpaRepository.findPendingForUpdate(
                MapAnalysisJobStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    @Override
    public Optional<MapAnalysisJob> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<MapAnalysisJob> findLatestByMapId(Long mapId) {
        return jpaRepository.findFirstByMapIdOrderByIdDesc(mapId);
    }

    @Override
    public void cancelActiveByMapId(Long mapId) {
        jpaRepository.findAllByMapIdAndStatusIn(
                mapId,
                List.of(
                        MapAnalysisJobStatus.PENDING,
                        MapAnalysisJobStatus.PROCESSING
                )
        )
                .forEach(MapAnalysisJob::cancel);
    }
}
