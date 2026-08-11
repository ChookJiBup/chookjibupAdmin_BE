package com.example.chookjibupadmin.map.roadmap.domain;

import java.util.Optional;

public interface FestivalRoadmapRepository {

    FestivalRoadmap save(FestivalRoadmap roadmap);

    Optional<FestivalRoadmap> findByFestivalId(Long festivalId);

    Optional<FestivalRoadmap> findByFestivalIdForUpdate(Long festivalId);

    void delete(FestivalRoadmap roadmap);
}
