package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MapAnalysisQueueApplicationService {
    private final MapAnalysisJobService jobService;
    private final FestivalRoadmapService roadmapService;
    private final MapAnalysisProperties properties;

    public MapAnalysisJob enqueueInitial(FestivalMap map) {
        roadmapService.save(FestivalRoadmap.create(
                map.getFestivalId(),map.getId(),map.getCreatedByAdminId()));
        return createJob(map);
    }

    public MapAnalysisJob enqueueReplacement(FestivalMap previous,FestivalMap replacement) {
        jobService.cancelActive(previous.getId());
        FestivalRoadmap roadmap=roadmapService.getByFestivalId(replacement.getFestivalId());
        roadmap.replaceMap(replacement.getId());
        return createJob(replacement);
    }

    public void cancel(FestivalMap map){jobService.cancelActive(map.getId());}

    private MapAnalysisJob createJob(FestivalMap map) {
        return jobService.save(MapAnalysisJob.pending(map.getId(),properties.providerOrDefault(),
                properties.modelOrDefault(),map.getAnalysisImageKey().getValue(),
                map.getAnalysisChecksumSha256().getValue(),
                map.getAnalysisImageDimensions().getWidth(),map.getAnalysisImageDimensions().getHeight()));
    }
}
