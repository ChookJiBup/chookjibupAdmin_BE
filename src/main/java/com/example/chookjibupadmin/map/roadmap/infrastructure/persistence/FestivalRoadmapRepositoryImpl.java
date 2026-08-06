package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmapRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalRoadmapRepositoryImpl implements FestivalRoadmapRepository {

    private final FestivalRoadmapJpaRepository jpaRepository;

    @Override
    public FestivalRoadmap save(FestivalRoadmap roadmap) {
        return jpaRepository.save(roadmap);
    }

    @Override
    public Optional<FestivalRoadmap> findByFestivalId(Long id) {
        return jpaRepository.findByFestivalId(id);
    }

    @Override
    public Optional<FestivalRoadmap> findByFestivalIdForUpdate(Long id) {
        return jpaRepository.findByFestivalIdForUpdate(id);
    }
}
