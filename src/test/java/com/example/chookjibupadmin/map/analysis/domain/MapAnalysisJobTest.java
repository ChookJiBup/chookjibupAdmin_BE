package com.example.chookjibupadmin.map.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapAnalysisJobTest {

    @Test
    @DisplayName("대기 중인 분석 작업을 시작하고 완료한다")
    void success_Complete_FromPending() {
        // given
        MapAnalysisJob job = pendingJob();

        // when
        job.start();
        job.complete(3, 2, 1, "[]");

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.COMPLETED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getAcceptedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("취소된 분석 작업은 실패 상태로 변경하지 않는다")
    void success_Fail_CancelledJobUnchanged() {
        // given
        MapAnalysisJob job = pendingJob();
        job.cancel();

        // when
        job.fail("ERROR", "ignored");

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.CANCELLED);
    }

    @Test
    @DisplayName("재시도하면 지수 백오프 시각과 실패 정보를 기록한다")
    void success_Retry() {
        // given
        MapAnalysisJob job = pendingJob();
        job.start();

        // when
        job.retry("OPENAI_HTTP_429", "too many requests");

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.PENDING);
        assertThat(job.getFailureCode()).isEqualTo("OPENAI_HTTP_429");
        assertThat(job.getNextAttemptAt()).isNotNull();
    }

    private MapAnalysisJob pendingJob() {
        return MapAnalysisJob.pending(
                1L,
                "openai",
                "gpt-5.6",
                "key",
                "a".repeat(64),
                100,
                200
        );
    }
}
