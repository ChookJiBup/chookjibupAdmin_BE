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
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final MapAnchorProjector anchorProjector;
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

        // 지도에 앵커가 있으면 편집 화면과 운영 대시보드가 읽는 위경도(2.0)로 옮겨 저장한다.
        String schemaVersion = map.geometrySchemaVersion();
        List<RoadmapNode> accepted = new ArrayList<>();
        List<Map<String, Object>> rejected = new ArrayList<>();
        int order = 0;

        for (AnalyzedMapNode candidate : result.nodes()) {
            if (!validator.isValid(candidate)) {
                rejected.add(rejected(order, "INVALID_GEOMETRY"));
                order++;
                continue;
            }

            StoredGeometry stored = toStoredGeometry(map, schemaVersion, candidate);
            if (stored == null) {
                rejected.add(rejected(order, "PROJECTION_FAILED"));
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
                        stored.geometryType(),
                        objectMapper.writeValueAsString(stored.geometry()),
                        candidate.confidence(),
                        candidate.recognizedText(),
                        order,
                        schemaVersion
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

    /**
     * 저장할 geometry를 정한다. 앵커가 없으면 AI가 준 정규화 좌표를 그대로 두고,
     * 있으면 위경도로 투영한다. 투영 결과가 스키마 검증을 통과하지 못하면 null이다.
     */
    private StoredGeometry toStoredGeometry(
            FestivalMap map,
            String schemaVersion,
            AnalyzedMapNode candidate
    ) {
        if (!"2.0".equals(schemaVersion) || !map.hasImageAnchor()) {
            return new StoredGeometry(
                    candidate.geometryType(),
                    candidate.geometry()
            );
        }

        MapAnchorProjector.ProjectedGeometry projected = anchorProjector.project(
                map.getImageAnchor(),
                map.getAnalysisImageDimensions().getWidth(),
                map.getAnalysisImageDimensions().getHeight(),
                candidate.geometryType(),
                candidate.geometry()
        );
        if (projected == null || !validator.isValid(
                schemaVersion,
                projected.geometryType(),
                projected.geometry()
        )) {
            return null;
        }
        return new StoredGeometry(projected.geometryType(), projected.geometry());
    }

    private record StoredGeometry(GeometryType geometryType, JsonNode geometry) {
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
