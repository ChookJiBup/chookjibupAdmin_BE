package com.example.chookjibupadmin.map.roadmap.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoadmapNodeRepository {

    List<RoadmapNode> saveAll(Iterable<RoadmapNode> nodes);

    RoadmapNode save(RoadmapNode node);

    List<RoadmapNode> findAllByRoadmapIdAndMapId(Long roadmapId, Long mapId);

    List<RoadmapNode> findAllById(Collection<Long> ids);

    Optional<RoadmapNode> findByPublicIdAndMapId(UUID publicId, Long mapId);

    Optional<RoadmapNode> findByPublicIdAndMapIdForUpdate(UUID publicId, Long mapId);

    void deleteAll(Iterable<RoadmapNode> nodes);

    void deleteAllByRoadmapId(Long roadmapId);
}
