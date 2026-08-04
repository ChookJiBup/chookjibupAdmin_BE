package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor
public class FestivalRoadmapRepositoryImpl implements FestivalRoadmapRepository {
    private final FestivalRoadmapJpaRepository jpaRepository;
    public FestivalRoadmap save(FestivalRoadmap roadmap){return jpaRepository.save(roadmap);}
    public Optional<FestivalRoadmap> findByFestivalId(Long id){return jpaRepository.findByFestivalId(id);}
}
