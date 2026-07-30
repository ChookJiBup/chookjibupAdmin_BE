package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.MapAnalysisJob;
import com.example.demoadmin.map.command.domain.MapAnalysisJobRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MapAnalysisJobRepositoryImpl implements MapAnalysisJobRepository {

    private final MapAnalysisJobJpaRepository jpaRepository;

    @Override
    public MapAnalysisJob save(MapAnalysisJob analysisJob) {
        return jpaRepository.save(analysisJob);
    }

    @Override
    public Optional<MapAnalysisJob> findById(Long analysisJobId) {
        return jpaRepository.findById(analysisJobId);
    }

    @Override
    public Optional<MapAnalysisJob> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }
}
