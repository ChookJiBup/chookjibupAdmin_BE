package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNodeRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoadmapNodeRepositoryImpl implements RoadmapNodeRepository {

    private final RoadmapNodeJpaRepository jpaRepository;

    @Override
    public List<RoadmapNode> saveAll(Iterable<RoadmapNode> nodes) {
        return jpaRepository.saveAll(nodes);
    }

    @Override
    public RoadmapNode save(RoadmapNode node) {
        return jpaRepository.save(node);
    }

    @Override
    public List<RoadmapNode> findAllByRoadmapIdAndMapId(
            Long roadmapId,
            Long mapId
    ) {
        return jpaRepository.findAllByRoadmapIdAndMapIdOrderBySortOrder(
                roadmapId,
                mapId
        );
    }

    @Override
    public List<RoadmapNode> findAllById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(ids);
    }

    @Override
    public Optional<RoadmapNode> findByPublicIdAndMapId(UUID publicId, Long mapId) {
        return jpaRepository.findByPublicIdAndMapId(publicId, mapId);
    }

    @Override
    public Optional<RoadmapNode> findByPublicIdAndMapIdForUpdate(
            UUID publicId,
            Long mapId
    ) {
        return jpaRepository.findByPublicIdAndMapIdForUpdate(publicId, mapId);
    }

    @Override
    public void deleteAll(Iterable<RoadmapNode> nodes) {
        jpaRepository.deleteAll(nodes);
    }

    @Override
    public void deleteAllByRoadmapId(Long roadmapId) {
        jpaRepository.deleteAllByRoadmapId(roadmapId);
    }
}
