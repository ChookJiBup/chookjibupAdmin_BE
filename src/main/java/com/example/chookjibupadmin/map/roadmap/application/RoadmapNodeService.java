package com.example.chookjibupadmin.map.roadmap.application;

import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadmapNodeService {

    private final RoadmapNodeRepository repository;

    @Transactional
    public List<RoadmapNode> saveAll(Iterable<RoadmapNode> nodes) {
        return repository.saveAll(nodes);
    }

    public List<RoadmapNode> findAll(Long roadmapId, Long mapId) {
        return repository.findAllByRoadmapIdAndMapId(
                roadmapId,
                mapId
        );
    }

    @Transactional
    public void deleteAll(Iterable<RoadmapNode> nodes) {
        repository.deleteAll(nodes);
    }

    @Transactional
    public void deleteAllByRoadmapId(Long roadmapId) {
        repository.deleteAllByRoadmapId(roadmapId);
    }
}
