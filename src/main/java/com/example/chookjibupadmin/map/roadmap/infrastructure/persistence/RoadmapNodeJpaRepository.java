package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapNodeJpaRepository
        extends JpaRepository<RoadmapNode, Long> {

    List<RoadmapNode> findAllByRoadmapIdAndMapIdOrderBySortOrder(
            Long roadmapId,
            Long mapId
    );

    void deleteAllByRoadmapId(Long roadmapId);
}
