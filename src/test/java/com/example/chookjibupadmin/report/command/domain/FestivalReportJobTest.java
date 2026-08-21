package com.example.chookjibupadmin.report.command.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
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
}
