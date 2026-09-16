package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothInfoJpaRepository extends JpaRepository<BoothInfo, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from BoothInfo b where b.id = :id")
    Optional<BoothInfo> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<BoothInfo> findAllByFestivalIdOrderByIdAsc(Long festivalId);

    Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    );

    long countByFestivalId(Long festivalId);

    List<BoothInfo> findAllByRoadmapNodeIdIn(List<Long> roadmapNodeIds);
}
