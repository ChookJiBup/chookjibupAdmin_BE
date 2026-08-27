package com.example.chookjibupadmin.map.roadmap.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNodeRepository;
import java.util.List;
import java.util.UUID;
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

    @Transactional
    public RoadmapNode save(RoadmapNode node) {
        return repository.save(node);
    }

    public List<RoadmapNode> findAll(Long roadmapId, Long mapId) {
        return repository.findAllByRoadmapIdAndMapId(
                roadmapId,
                mapId
        );
    }

    public RoadmapNode getByPublicIdAndMapId(UUID publicId, Long mapId) {
        return repository.findByPublicIdAndMapId(publicId, mapId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROADMAP_NODE_NOT_FOUND));
    }

    @Transactional
    public RoadmapNode getByPublicIdAndMapIdForUpdate(UUID publicId, Long mapId) {
        return repository.findByPublicIdAndMapIdForUpdate(publicId, mapId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROADMAP_NODE_NOT_FOUND));
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
