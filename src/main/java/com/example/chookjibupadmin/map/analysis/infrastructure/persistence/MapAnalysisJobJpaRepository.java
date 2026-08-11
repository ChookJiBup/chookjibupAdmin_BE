package com.example.chookjibupadmin.map.analysis.infrastructure.persistence;

import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MapAnalysisJobJpaRepository extends JpaRepository<MapAnalysisJob, Long> {

    Optional<MapAnalysisJob> findByPublicId(UUID publicId);

    Optional<MapAnalysisJob> findFirstByMapIdOrderByIdDesc(Long mapId);

    Collection<MapAnalysisJob> findAllByMapIdAndStatusIn(
            Long mapId,
            Collection<MapAnalysisJobStatus> statuses
    );

    void deleteAllByMapIdIn(Collection<Long> mapIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select j
        from MapAnalysisJob j
        where j.status = :status
          and (j.nextAttemptAt is null or j.nextAttemptAt <= :now)
        order by j.id
        """)
    List<MapAnalysisJob> findPendingForUpdate(
            MapAnalysisJobStatus status,
            LocalDateTime now,
            Pageable pageable
    );
}
