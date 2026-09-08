package com.example.chookjibupadmin.report.command.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalReportJobTest {

    @Test
    @DisplayName("대기 작업을 처리 후 완료한다")
    void success_StartAndComplete() {
        FestivalReportJob job = FestivalReportJob.pending(
                1L,
                "disabled",
                "gpt-5.6",
                "1.0",
                "1.0"
        );

        job.start();
        job.updateProgress(1, "1일차 분석 중");
        job.complete();

        assertThat(job.getStatus()).isEqualTo(FestivalReportJobStatus.COMPLETED);
        assertThat(job.getProgressDayIndex()).isEqualTo(1);
        assertThat(job.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도 가능한 실패는 다시 대기 상태가 된다")
    void success_Retry() {
        FestivalReportJob job = FestivalReportJob.pending(
                1L,
                "openai",
                "gpt-5.6",
                "1.0",
                "1.0"
        );
        job.start();
        job.retry("OPENAI_HTTP_429", "rate limited");

        assertThat(job.getStatus()).isEqualTo(FestivalReportJobStatus.PENDING);
        assertThat(job.getFailureCode()).isEqualTo("OPENAI_HTTP_429");
        assertThat(job.getNextAttemptAt()).isNotNull();
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("처리 중인 작업은 사유를 남기고 실패로 종료한다")
        void success_Fail_FromProcessing() {
            // given
            FestivalReportJob job = processing();

            // when
            job.fail("REPORT_ANALYSIS_TIMEOUT", "제한 시간 초과");

            // then
            assertThat(job.getStatus())
                    .isEqualTo(FestivalReportJobStatus.FAILED);
            assertThat(job.getFailureCode())
                    .isEqualTo("REPORT_ANALYSIS_TIMEOUT");
            assertThat(job.getFailureMessage()).isEqualTo("제한 시간 초과");
            assertThat(job.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 완료된 작업은 실패로 되돌리지 않는다")
        void success_Fail_IgnoredWhenCompleted() {
            // given
            FestivalReportJob job = processing();
            job.complete();

            // when
            job.fail("REPORT_INTERNAL_ERROR", "내부 오류");

            // then
            assertThat(job.getStatus())
                    .isEqualTo(FestivalReportJobStatus.COMPLETED);
            assertThat(job.getFailureCode()).isNull();
        }

        @Test
        @DisplayName("취소된 작업은 실패로 되돌리지 않는다")
        void success_Fail_IgnoredWhenCancelled() {
            // given
            FestivalReportJob job = processing();
            job.cancel();

            // when
            job.fail("REPORT_INTERNAL_ERROR", "내부 오류");

            // then
            assertThat(job.getStatus())
                    .isEqualTo(FestivalReportJobStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("isTimedOut")
    class IsTimedOut {

        @Test
        @DisplayName("제한 시간을 넘겨 처리 중이면 시간 초과로 판정한다")
        void success_IsTimedOut_ProcessingOverLimit() {
            // given
            FestivalReportJob job = processing();
            LocalDateTime startedAt = job.getStartedAt();

            // when
            boolean timedOut = job.isTimedOut(
                    startedAt.plusMinutes(8),
                    Duration.ofMinutes(7)
            );

            // then
            assertThat(timedOut).isTrue();
        }

        @Test
        @DisplayName("제한 시간 안이면 시간 초과가 아니다")
        void success_IsTimedOut_WithinLimit() {
            // given
            FestivalReportJob job = processing();
            LocalDateTime startedAt = job.getStartedAt();

            // when
            boolean timedOut = job.isTimedOut(
                    startedAt.plusMinutes(6),
                    Duration.ofMinutes(7)
            );

            // then
            assertThat(timedOut).isFalse();
        }

        @Test
        @DisplayName("경계값인 제한 시간 정각은 시간 초과로 판정한다")
        void success_IsTimedOut_AtLimit() {
            // given
            FestivalReportJob job = processing();
            LocalDateTime startedAt = job.getStartedAt();

            // when
            boolean timedOut = job.isTimedOut(
                    startedAt.plusMinutes(7),
                    Duration.ofMinutes(7)
            );

            // then
            assertThat(timedOut).isTrue();
        }

        @Test
        @DisplayName("처리 중이 아니면 시간 초과로 판정하지 않는다")
        void success_IsTimedOut_NotProcessing() {
            // given
            FestivalReportJob job = FestivalReportJob.pending(
                    1L,
                    "openai",
                    "gpt-5.6",
                    "1.0",
                    "1.0"
            );

            // when
            boolean timedOut = job.isTimedOut(
                    LocalDateTime.now().plusDays(1),
                    Duration.ofMinutes(7)
            );

            // then
            assertThat(timedOut).isFalse();
        }

        @Test
        @DisplayName("제한 시간이 없으면 시간 초과로 판정하지 않는다")
        void success_IsTimedOut_NoTimeout() {
            // given
            FestivalReportJob job = processing();

            // when & then
            assertThat(job.isTimedOut(LocalDateTime.now(), null)).isFalse();
            assertThat(job.isTimedOut(LocalDateTime.now(), Duration.ZERO))
                    .isFalse();
        }
    }

    private static FestivalReportJob processing() {
        FestivalReportJob job = FestivalReportJob.pending(
                1L,
                "openai",
                "gpt-5.6",
                "1.0",
                "1.0"
        );
        job.start();
        return job;
    }
}
