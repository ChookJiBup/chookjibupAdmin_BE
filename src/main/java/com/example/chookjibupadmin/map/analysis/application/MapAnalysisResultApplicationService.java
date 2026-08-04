package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.application.dto.*;
import com.example.chookjibupadmin.map.analysis.domain.*;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.*;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class MapAnalysisResultApplicationService {
    private final MapAnalysisJobService jobService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;
    private final MapGeometryValidator validator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void complete(UUID jobPublicId, MapAnalysisResult result) {
        MapAnalysisJob job=jobService.getByPublicId(jobPublicId);
        if(job.getStatus()!=MapAnalysisJobStatus.PROCESSING)return;
        FestivalMap map=mapService.getById(job.getMapId());
        try { map.validateReadable(); } catch(RuntimeException e){job.cancel();jobService.save(job);return;}
        FestivalRoadmap roadmap=roadmapService.getByFestivalId(map.getFestivalId());
        if(!roadmap.getCurrentMapId().equals(map.getId())){job.cancel();jobService.save(job);return;}
        List<RoadmapNode> accepted=new ArrayList<>(); List<Map<String,Object>> rejected=new ArrayList<>();
        int order=0;
        for(AnalyzedMapNode candidate:result.nodes()) {
            if(!validator.isValid(candidate)) { rejected.add(Map.of("index",order,"reason","INVALID_GEOMETRY")); order++; continue; }
            try { accepted.add(RoadmapNode.ai(roadmap.getId(),map.getId(),job.getId(),candidate.nodeType(),
                    candidate.name().trim(),candidate.geometryType(),objectMapper.writeValueAsString(candidate.geometry()),
                    candidate.confidence(),candidate.recognizedText(),order));
            } catch(Exception e){rejected.add(Map.of("index",order,"reason","SERIALIZATION_FAILED"));}
            order++;
        }
        nodeService.saveAll(accepted);
        roadmap.analysisCompleted(); roadmapService.save(roadmap);
        try { job.complete(result.nodes().size(),accepted.size(),rejected.size(),objectMapper.writeValueAsString(rejected)); }
        catch(Exception e){job.complete(result.nodes().size(),accepted.size(),rejected.size(),"[]");}
        jobService.save(job);
    }
}
