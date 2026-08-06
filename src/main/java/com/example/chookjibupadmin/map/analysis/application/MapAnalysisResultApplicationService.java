package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MapAnalysisResultApplicationService {

    private final MapAnalysisJobService jobService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;
    private final MapGeometryValidator validator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void complete(UUID jobPublicId, MapAnalysisResult result) {
        MapAnalysisJob job = jobService.getByPublicId(jobPublicId);
        if (job.getStatus() != MapAnalysisJobStatus.PROCESSING) {
            return;
        }

        FestivalMap map = mapService.getById(job.getMapId());
        if (!isCurrentAnalysisSource(job, map)) {
            cancel(job);
            return;
        }

        FestivalRoadmap roadmap = roadmapService.getByFestivalId(
                map.getFestivalId()
        );
        if (!roadmap.getCurrentMapId().equals(map.getId())) {
            cancel(job);
            return;
        }

        List<RoadmapNode> accepted = new ArrayList<>();
        List<Map<String, Object>> rejected = new ArrayList<>();
        int order = 0;

        for (AnalyzedMapNode candidate : result.nodes()) {
            if (!validator.isValid(candidate)) {
                rejected.add(rejected(order, "INVALID_GEOMETRY"));
                order++;
                continue;
            }

            try {
                accepted.add(RoadmapNode.ai(
                        roadmap.getId(),
                        map.getId(),
                        job.getId(),
                        candidate.nodeType(),
                        candidate.name().trim(),
                        candidate.geometryType(),
                        objectMapper.writeValueAsString(
                                candidate.geometry()
                        ),
                        candidate.confidence(),
                        candidate.recognizedText(),
                        order
                ));
            } catch (Exception exception) {
                rejected.add(rejected(order, "SERIALIZATION_FAILED"));
            }

            order++;
        }

        nodeService.saveAll(accepted);
        roadmap.analysisCompleted();
        roadmapService.save(roadmap);
        job.complete(
                result.nodes().size(),
                accepted.size(),
                rejected.size(),
                serialize(rejected)
        );
        jobService.save(job);
    }

    private boolean isCurrentAnalysisSource(
            MapAnalysisJob job,
            FestivalMap map
    ) {
        try {
            map.validateReadable();
        } catch (RuntimeException exception) {
            return false;
        }

        return job.getInputImageKey().equals(
                map.getAnalysisImageKey().getValue()
        ) && job.getInputChecksumSha256().equals(
                map.getAnalysisChecksumSha256().getValue()
        ) && job.getInputImageWidth()
                == map.getAnalysisImageDimensions().getWidth()
                && job.getInputImageHeight()
                == map.getAnalysisImageDimensions().getHeight();
    }

    private void cancel(MapAnalysisJob job) {
        job.cancel();
        jobService.save(job);
    }

    private Map<String, Object> rejected(int index, String reason) {
        return Map.of(
                "index", index,
                "reason", reason
        );
    }

    private String serialize(List<Map<String, Object>> rejected) {
        try {
            return objectMapper.writeValueAsString(rejected);
        } catch (Exception exception) {
            return "[]";
        }
    }
}
