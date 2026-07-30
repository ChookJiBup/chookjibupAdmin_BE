package com.example.demoadmin.map.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapAnalysisJobTest {

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        @DisplayName("대기 중인 분석 작업을 시작한다")
        void success_Start() {
            // given
            MapAnalysisJob analysisJob = analysisJob();

            // when
            analysisJob.start();

            // then
            assertThat(analysisJob.getStatus()).isEqualTo(MapAnalysisStatus.ANALYZING);
        }

        @Test
        @DisplayName("이미 완료된 분석 작업은 시작할 수 없다")
        void fail_Start_CustomException() {
            // given
            MapAnalysisJob analysisJob = analysisJob();
            analysisJob.start();
            analysisJob.complete();

            // when & then
            assertThatThrownBy(analysisJob::start)
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MAP_ANALYSIS_STATUS_INVALID.getMessage());
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("분석 작업을 실패 처리한다")
        void success_Fail() {
            // given
            MapAnalysisJob analysisJob = analysisJob();

            // when
            analysisJob.fail("분석 실패");

            // then
            assertThat(analysisJob.getStatus()).isEqualTo(MapAnalysisStatus.FAILED);
            assertThat(analysisJob.getFailureReason()).isEqualTo("분석 실패");
        }
    }

    private MapAnalysisJob analysisJob() {
        return MapAnalysisJob.create(1L, 1L);
    }
}
