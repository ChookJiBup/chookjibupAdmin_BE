package com.example.chookjibupadmin.report.command.infrastructure.persistence;

import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobRepository;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalReportJobRepositoryImpl
        implements FestivalReportJobRepository {

    private static final List<FestivalReportJobStatus> ACTIVE_STATUSES = List.of(
            FestivalReportJobStatus.PENDING,
            FestivalReportJobStatus.PROCESSING
    );

    private final FestivalReportJobJpaRepository jpaRepository;

    @Override
    public FestivalReportJob save(FestivalReportJob job) {
        return jpaRepository.save(job);
    }

    @Override
    public Optional<FestivalReportJob> findFirstPending() {
        return jpaRepository.findPendingForUpdate(
                FestivalReportJobStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    @Override
    public Optional<FestivalReportJob> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<FestivalReportJob> findLatestByFestivalId(Long festivalId) {
        return jpaRepository.findFirstByFestivalIdOrderByIdDesc(festivalId);
    }

    @Override
    public boolean existsActiveByFestivalId(Long festivalId) {
        return jpaRepository.existsByFestivalIdAndStatusIn(
                festivalId,
                ACTIVE_STATUSES
        );
    }

    @Override
    public void cancelActiveByFestivalId(Long festivalId) {
        jpaRepository.findAllByFestivalIdAndStatusIn(
                festivalId,
                ACTIVE_STATUSES
        ).forEach(job -> {
            job.cancel();
            jpaRepository.save(job);
        });
    }
}
