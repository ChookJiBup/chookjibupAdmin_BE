package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.MapAnalysisJobRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisJobServiceTest {

    @InjectMocks
    private MapAnalysisJobService mapAnalysisJobService;

    @Mock
    private MapAnalysisJobRepository mapAnalysisJobRepository;

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("분석 작업이 없으면 예외를 던진다")
        void fail_GetById_CustomException() {
            // given
            Long analysisJobId = 1L;
            given(mapAnalysisJobRepository.findById(analysisJobId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mapAnalysisJobService.getById(analysisJobId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MAP_ANALYSIS_JOB_NOT_FOUND.getMessage());
        }
    }
}
