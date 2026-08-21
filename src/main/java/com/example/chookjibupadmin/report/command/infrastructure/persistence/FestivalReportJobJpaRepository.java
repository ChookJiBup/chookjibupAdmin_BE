package com.example.chookjibupadmin.report.command.infrastructure.persistence;

import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
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

public interface FestivalReportJobJpaRepository
        extends JpaRepository<FestivalReportJob, Long> {

    Optional<FestivalReportJob> findByPublicId(UUID publicId);

    Optional<FestivalReportJob> findFirstByFestivalIdOrderByIdDesc(Long festivalId);

    List<FestivalReportJob> findAllByFestivalIdAndStatusIn(
            Long festivalId,
            Collection<FestivalReportJobStatus> statuses
    );

    boolean existsByFestivalIdAndStatusIn(
            Long festivalId,
            Collection<FestivalReportJobStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select j
        from FestivalReportJob j
        where j.status = :status
          and (j.nextAttemptAt is null or j.nextAttemptAt <= :now)
        order by j.id
        """)
    List<FestivalReportJob> findPendingForUpdate(
            FestivalReportJobStatus status,
            LocalDateTime now,
            Pageable pageable
    );
}
