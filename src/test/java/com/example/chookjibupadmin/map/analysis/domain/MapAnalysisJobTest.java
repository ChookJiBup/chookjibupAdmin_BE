package com.example.chookjibupadmin.map.analysis.domain;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class MapAnalysisJobTest {
    @Test void transitionsFromPendingToCompleted(){
        MapAnalysisJob job=MapAnalysisJob.pending(1L,"openai","gpt-5.6","key","a".repeat(64),100,200);
        job.start(); job.complete(3,2,1,"[]");
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.COMPLETED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getAcceptedCount()).isEqualTo(2);
    }
    @Test void cancelledJobDoesNotBecomeFailed(){
        MapAnalysisJob job=MapAnalysisJob.pending(1L,"openai","gpt-5.6","key","a".repeat(64),100,200);
        job.cancel(); job.fail("ERROR","ignored");
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.CANCELLED);
    }
}
