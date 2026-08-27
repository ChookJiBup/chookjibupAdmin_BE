package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothInfoJpaRepository extends JpaRepository<BoothInfo, Long> {

    List<BoothInfo> findAllByFestivalIdOrderByIdAsc(Long festivalId);

    Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    );

    long countByFestivalId(Long festivalId);
}
