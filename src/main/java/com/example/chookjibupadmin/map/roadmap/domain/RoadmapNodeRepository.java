package com.example.chookjibupadmin.map.roadmap.domain;

import java.util.List;

public interface RoadmapNodeRepository {

    List<RoadmapNode> saveAll(Iterable<RoadmapNode> nodes);

    List<RoadmapNode> findAllByRoadmapIdAndMapId(Long roadmapId, Long mapId);

    void deleteAll(Iterable<RoadmapNode> nodes);

    void deleteAllByRoadmapId(Long roadmapId);
}
