package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobRepository;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisJobServiceTest {

    @InjectMocks
    private MapAnalysisJobService jobService;

    @Mock
    private MapAnalysisJobRepository repository;

    @Test
    @DisplayName("가장 오래된 실행 가능한 대기 작업을 선점한다")
    void success_ClaimPending() {
        // given
        MapAnalysisJob job = pendingJob();
        given(repository.findFirstPending()).willReturn(Optional.of(job));

        // when
        Optional<MapAnalysisJob> claimed = jobService.claimPending();

        // then
        assertThat(claimed).contains(job);
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.PROCESSING);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        then(repository).should().findFirstPending();
    }

    @Test
    @DisplayName("분석 작업 공개 식별자가 없으면 조회 예외를 반환한다")
    void fail_GetByPublicId_CustomException() {
        // given
        UUID jobId = UUID.randomUUID();
        given(repository.findByPublicId(jobId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jobService.getByPublicId(jobId))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND)
                );
    }

    private MapAnalysisJob pendingJob() {
        return MapAnalysisJob.pending(
                1L,
                "openai",
                "gpt-5.6",
                "analysis-key",
                "a".repeat(64),
                1200,
                800
        );
    }
}
