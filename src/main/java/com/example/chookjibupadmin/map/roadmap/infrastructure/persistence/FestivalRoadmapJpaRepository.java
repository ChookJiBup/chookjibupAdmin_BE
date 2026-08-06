package com.example.chookjibupadmin.map.roadmap.infrastructure.persistence;

import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalRoadmapJpaRepository
        extends JpaRepository<FestivalRoadmap, Long> {

    Optional<FestivalRoadmap> findByFestivalId(Long festivalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select roadmap from FestivalRoadmap roadmap where roadmap.festivalId = :festivalId")
    Optional<FestivalRoadmap> findByFestivalIdForUpdate(
            @Param("festivalId") Long festivalId
    );
}
