package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNodeRepository;
import java.util.List;
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
    public List<RoadmapNode> findAllByRoadmapIdAndMapId(
            Long roadmapId,
            Long mapId
    ) {
        return jpaRepository.findAllByRoadmapIdAndMapIdOrderBySortOrder(
                roadmapId,
                mapId
        );
    }
}
