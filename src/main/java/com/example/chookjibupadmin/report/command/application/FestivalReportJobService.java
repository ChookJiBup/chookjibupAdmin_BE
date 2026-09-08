package com.example.chookjibupadmin.report.command.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.command.application.dto.FestivalReportJobReleaseResult;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobRepository;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * 제한 시간을 넘겨 회수된 작업에 남기는 실패 코드이다.
     */
    public static final String TIMEOUT_FAILURE_CODE = "REPORT_ANALYSIS_TIMEOUT";

    private static final String TIMEOUT_FAILURE_MESSAGE =
            "결과 보고서 분석이 제한 시간 안에 끝나지 않았습니다.";
    private static final int TIMEOUT_RELEASE_BATCH_SIZE = 50;

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

    /**
     * 처리 중인 작업의 진행 상황을 저장한다.
     *
     * <p>워커가 들고 있던 detached 인스턴스를 병합하면 {@code @Version}이 어긋나
     * 낙관적 락 예외로 작업이 PROCESSING에 갇히므로, 항상 다시 조회해 갱신한다.</p>
     */
    @Transactional
    public void updateProgress(UUID publicId, Integer dayIndex, String message) {
        repository.findByPublicId(publicId)
                .ifPresent(job -> job.updateProgress(dayIndex, message));
    }

    /**
     * 처리 중인 작업을 완료로 전환하고 실제로 전환됐는지 반환한다.
     */
    @Transactional
    public boolean complete(UUID publicId) {
        return repository.findByPublicId(publicId)
                .map(job -> {
                    job.complete();
                    return job.getStatus() == FestivalReportJobStatus.COMPLETED;
                })
                .orElse(false);
    }

    /**
     * 실패 사유를 남기고 재시도 대기 또는 실패로 전환한 뒤 재시도 여부를 반환한다.
     */
    @Transactional
    public boolean recordFailure(
            UUID publicId,
            String code,
            String message,
            boolean retryable,
            int maxAttempts
    ) {
        return repository.findByPublicId(publicId)
                .map(job -> applyFailure(job, code, message, retryable, maxAttempts))
                .orElse(false);
    }

    /**
     * 제한 시간을 넘긴 채 처리 중으로 남은 작업을 재시도 대기 또는 실패로 회수한다.
     */
    @Transactional
    public List<FestivalReportJobReleaseResult> releaseTimedOut(
            Duration timeout,
            int maxAttempts
    ) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<FestivalReportJobReleaseResult> released = new ArrayList<>();
        for (FestivalReportJob job : repository.findProcessingStartedBefore(
                now.minus(timeout),
                TIMEOUT_RELEASE_BATCH_SIZE
        )) {
            if (!job.isTimedOut(now, timeout)) {
                continue;
            }

            boolean retrying = applyFailure(
                    job,
                    TIMEOUT_FAILURE_CODE,
                    TIMEOUT_FAILURE_MESSAGE,
                    true,
                    maxAttempts
            );
            released.add(new FestivalReportJobReleaseResult(
                    job.getPublicId(),
                    job.getFestivalId(),
                    retrying
            ));
        }
        return released;
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

    private boolean applyFailure(
            FestivalReportJob job,
            String code,
            String message,
            boolean retryable,
            int maxAttempts
    ) {
        if (!job.getStatus().isActive()) {
            return false;
        }

        boolean retrying = retryable && job.getAttemptCount() < maxAttempts;
        if (retrying) {
            job.retry(code, message);
        } else {
            job.fail(code, message);
        }
        return retrying;
    }
}
