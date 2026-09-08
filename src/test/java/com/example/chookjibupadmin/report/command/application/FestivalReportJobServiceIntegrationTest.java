package com.example.chookjibupadmin.report.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.chookjibupadmin.report.command.application.dto.FestivalReportJobReleaseResult;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJob;
import com.example.chookjibupadmin.report.command.domain.FestivalReportJobStatus;
import com.example.chookjibupadmin.report.command.infrastructure.persistence.FestivalReportJobJpaRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 작업 상태 전이가 실제 커밋 경계를 넘어도 유지되는지 검증한다.
 *
 * <p>워커는 트랜잭션 밖에서 작업을 다루므로 {@code @Transactional} 없이 검증한다.</p>
 */
@SpringBootTest
class FestivalReportJobServiceIntegrationTest {

    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private FestivalReportJobService jobService;

    @Autowired
    private FestivalReportJobJpaRepository jobJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clear() {
        jobJpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("updateProgress")
    class UpdateProgress {

        @Test
        @DisplayName("선점한 작업의 진행 상황을 여러 번 갱신해도 실패하지 않는다")
        void success_UpdateProgress_RepeatedCalls() {
            // given
            jobService.save(pending(1L));
            FestivalReportJob claimed = jobService.claimPending().orElseThrow();
            UUID jobId = claimed.getPublicId();

            // when & then
            assertThatCode(() -> {
                jobService.updateProgress(jobId, 1, "1일차 분석 중");
                jobService.updateProgress(jobId, 2, "2일차 분석 중");
                jobService.updateProgress(jobId, 3, "3일차 분석 중");
            }).doesNotThrowAnyException();

            FestivalReportJob reloaded = reload(jobId);
            assertThat(reloaded.getStatus())
                    .isEqualTo(FestivalReportJobStatus.PROCESSING);
            assertThat(reloaded.getProgressDayIndex()).isEqualTo(3);
            assertThat(reloaded.getProgressMessage()).isEqualTo("3일차 분석 중");
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("진행 상황을 갱신한 뒤에도 완료로 전환한다")
        void success_Complete_AfterProgressUpdates() {
            // given
            jobService.save(pending(2L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();
            jobService.updateProgress(jobId, 1, "1일차 분석 중");
            jobService.updateProgress(jobId, 2, "2일차 분석 중");

            // when
            boolean completed = jobService.complete(jobId);

            // then
            assertThat(completed).isTrue();
            assertThat(reload(jobId).getStatus())
                    .isEqualTo(FestivalReportJobStatus.COMPLETED);
        }

        @Test
        @DisplayName("처리 중이 아닌 작업은 완료로 전환하지 않는다")
        void success_Complete_IgnoredWhenNotProcessing() {
            // given
            FestivalReportJob saved = jobService.save(pending(3L));

            // when
            boolean completed = jobService.complete(saved.getPublicId());

            // then
            assertThat(completed).isFalse();
            assertThat(reload(saved.getPublicId()).getStatus())
                    .isEqualTo(FestivalReportJobStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("재시도 여유가 있으면 사유를 남기고 대기로 되돌린다")
        void success_RecordFailure_Retry() {
            // given
            jobService.save(pending(4L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();
            jobService.updateProgress(jobId, 1, "1일차 분석 중");

            // when
            boolean retrying = jobService.recordFailure(
                    jobId,
                    "OPENAI_HTTP_429",
                    "rate limited",
                    true,
                    MAX_ATTEMPTS
            );

            // then
            assertThat(retrying).isTrue();
            FestivalReportJob reloaded = reload(jobId);
            assertThat(reloaded.getStatus())
                    .isEqualTo(FestivalReportJobStatus.PENDING);
            assertThat(reloaded.getFailureCode()).isEqualTo("OPENAI_HTTP_429");
            assertThat(reloaded.getFailureMessage()).isEqualTo("rate limited");
        }

        @Test
        @DisplayName("재시도할 수 없으면 사유를 남기고 실패로 종료한다")
        void success_RecordFailure_Fail() {
            // given
            jobService.save(pending(5L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();

            // when
            boolean retrying = jobService.recordFailure(
                    jobId,
                    "OPENAI_INVALID_RESPONSE",
                    "schema mismatch",
                    false,
                    MAX_ATTEMPTS
            );

            // then
            assertThat(retrying).isFalse();
            FestivalReportJob reloaded = reload(jobId);
            assertThat(reloaded.getStatus())
                    .isEqualTo(FestivalReportJobStatus.FAILED);
            assertThat(reloaded.getFailureCode())
                    .isEqualTo("OPENAI_INVALID_RESPONSE");
            assertThat(reloaded.getFailureMessage()).isEqualTo("schema mismatch");
        }
    }

    @Nested
    @DisplayName("releaseTimedOut")
    class ReleaseTimedOut {

        @Test
        @DisplayName("제한 시간을 넘긴 작업을 재시도 대기로 회수한다")
        void success_ReleaseTimedOut_Retry() {
            // given
            jobService.save(pending(6L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();
            backdateStartedAt(jobId, LocalDateTime.now().minusMinutes(30));

            // when
            List<FestivalReportJobReleaseResult> released =
                    jobService.releaseTimedOut(
                            Duration.ofMinutes(7),
                            MAX_ATTEMPTS
                    );

            // then
            assertThat(released).hasSize(1);
            assertThat(released.get(0).jobPublicId()).isEqualTo(jobId);
            assertThat(released.get(0).festivalId()).isEqualTo(6L);
            assertThat(released.get(0).retrying()).isTrue();

            FestivalReportJob reloaded = reload(jobId);
            assertThat(reloaded.getStatus())
                    .isEqualTo(FestivalReportJobStatus.PENDING);
            assertThat(reloaded.getFailureCode()).isEqualTo(
                    FestivalReportJobService.TIMEOUT_FAILURE_CODE
            );
            assertThat(reloaded.getFailureMessage()).isNotBlank();
        }

        @Test
        @DisplayName("재시도 한도를 넘긴 작업은 실패로 종료한다")
        void success_ReleaseTimedOut_Fail() {
            // given
            jobService.save(pending(7L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();
            backdateStartedAt(jobId, LocalDateTime.now().minusMinutes(30));
            setAttemptCount(jobId, MAX_ATTEMPTS);

            // when
            List<FestivalReportJobReleaseResult> released =
                    jobService.releaseTimedOut(
                            Duration.ofMinutes(7),
                            MAX_ATTEMPTS
                    );

            // then
            assertThat(released).hasSize(1);
            assertThat(released.get(0).retrying()).isFalse();
            FestivalReportJob reloaded = reload(jobId);
            assertThat(reloaded.getStatus())
                    .isEqualTo(FestivalReportJobStatus.FAILED);
            assertThat(reloaded.getFailureCode()).isEqualTo(
                    FestivalReportJobService.TIMEOUT_FAILURE_CODE
            );
            assertThat(reloaded.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("제한 시간 안의 작업은 회수하지 않는다")
        void success_ReleaseTimedOut_WithinLimit() {
            // given
            jobService.save(pending(8L));
            UUID jobId = jobService.claimPending().orElseThrow().getPublicId();

            // when
            List<FestivalReportJobReleaseResult> released =
                    jobService.releaseTimedOut(
                            Duration.ofMinutes(7),
                            MAX_ATTEMPTS
                    );

            // then
            assertThat(released).isEmpty();
            assertThat(reload(jobId).getStatus())
                    .isEqualTo(FestivalReportJobStatus.PROCESSING);
        }
    }

    private FestivalReportJob reload(UUID publicId) {
        return jobJpaRepository.findByPublicId(publicId).orElseThrow();
    }

    private void backdateStartedAt(UUID publicId, LocalDateTime startedAt) {
        jdbcTemplate.update(
                "update festival_report_job set started_at = ? "
                        + "where public_id = ?",
                startedAt,
                publicId
        );
    }

    private void setAttemptCount(UUID publicId, int attemptCount) {
        jdbcTemplate.update(
                "update festival_report_job set attempt_count = ? "
                        + "where public_id = ?",
                attemptCount,
                publicId
        );
    }

    private FestivalReportJob pending(Long festivalId) {
        return FestivalReportJob.pending(
                festivalId,
                "disabled",
                "gpt-5.6",
                "1.0",
                "1.0"
        );
    }
}
