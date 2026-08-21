package com.example.chookjibupadmin.report.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 결과 보고서 작업 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalReportJobService {

    private final FestivalReportJobRepository repository;

    @Transactional
    public FestivalReportJob save(FestivalReportJob job) {
        return repository.save(job);
    }

    @Transactional
    public Optional<FestivalReportJob> claimPending() {
        Optional<FestivalReportJob> job = repository.findFirstPending();
        job.ifPresent(FestivalReportJob::start);
        return job;
    }

    public FestivalReportJob getByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_REPORT_JOB_NOT_FOUND
                ));
    }

    public Optional<FestivalReportJob> findLatestByFestivalId(Long festivalId) {
        return repository.findLatestByFestivalId(festivalId);
    }

    public boolean existsActive(Long festivalId) {
        return repository.existsActiveByFestivalId(festivalId);
    }

    @Transactional
    public void cancelActive(Long festivalId) {
        repository.cancelActiveByFestivalId(festivalId);
    }
}
