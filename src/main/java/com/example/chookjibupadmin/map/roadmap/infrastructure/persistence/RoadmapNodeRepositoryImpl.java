package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor
public class RoadmapNodeRepositoryImpl implements RoadmapNodeRepository {
    private final RoadmapNodeJpaRepository jpaRepository;
    public List<RoadmapNode> saveAll(Iterable<RoadmapNode> nodes){return jpaRepository.saveAll(nodes);}
    public List<RoadmapNode> findAllByRoadmapIdAndMapId(Long roadmapId,Long mapId){
        return jpaRepository.findAllByRoadmapIdAndMapIdOrderBySortOrder(roadmapId,mapId);}
}
