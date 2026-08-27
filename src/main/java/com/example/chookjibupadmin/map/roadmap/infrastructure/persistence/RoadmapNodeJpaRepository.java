package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadmapNodeJpaRepository
        extends JpaRepository<RoadmapNode, Long> {

    List<RoadmapNode> findAllByRoadmapIdAndMapIdOrderBySortOrder(
            Long roadmapId,
            Long mapId
    );

    Optional<RoadmapNode> findByPublicIdAndMapId(UUID publicId, Long mapId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n
            from RoadmapNode n
            where n.publicId = :publicId
              and n.mapId = :mapId
            """)
    Optional<RoadmapNode> findByPublicIdAndMapIdForUpdate(
            @Param("publicId") UUID publicId,
            @Param("mapId") Long mapId
    );

    void deleteAllByRoadmapId(Long roadmapId);
}
