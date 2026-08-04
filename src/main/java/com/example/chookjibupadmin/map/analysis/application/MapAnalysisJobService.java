package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapAnalysisJobService {
    private final MapAnalysisJobRepository repository;
    @Transactional public MapAnalysisJob save(MapAnalysisJob job) { return repository.save(job); }
    @Transactional public Optional<MapAnalysisJob> claimPending() {
        Optional<MapAnalysisJob> job = repository.findFirstPending();
        job.ifPresent(MapAnalysisJob::start);
        return job;
    }
    public MapAnalysisJob getByPublicId(UUID id) { return repository.findByPublicId(id)
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND)); }
    public MapAnalysisJob getLatestByMapId(Long mapId) { return repository.findLatestByMapId(mapId)
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND)); }
    @Transactional public void cancelActive(Long mapId) { repository.cancelActiveByMapId(mapId); }
}
