package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRoadmapJpaRepository
        extends JpaRepository<FestivalRoadmap, Long> {

    Optional<FestivalRoadmap> findByFestivalId(Long festivalId);
}
