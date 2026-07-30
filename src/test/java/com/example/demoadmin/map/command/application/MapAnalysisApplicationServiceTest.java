package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import com.example.demoadmin.map.command.application.dto.MapAnalysisResultView;
import com.example.demoadmin.map.command.application.dto.PreparedMapAnalysis;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.application.port.MapImageAnalyzer;
import com.example.demoadmin.map.command.domain.MapAnalysisStatus;
import com.example.demoadmin.map.command.domain.MapStorageType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisApplicationServiceTest {

    @InjectMocks
    private MapAnalysisApplicationService mapAnalysisApplicationService;

    @Mock
    private MapAnalysisPersistenceService mapAnalysisPersistenceService;

    @Mock
    private MapImageAnalyzer mapImageAnalyzer;

    @Nested
    @DisplayName("analyzeTestMap")
    class AnalyzeTestMap {

        @Test
        @DisplayName("외부 분석을 트랜잭션 사이에서 실행하고 결과를 반영한다")
        void success_AnalyzeTestMap() {
            // given
            UUID festivalId = UUID.randomUUID();
            CreateTestMapAnalysisCommand command = command();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            PreparedMapAnalysis prepared = prepared();
            MapAnalysisResult analysisResult = new MapAnalysisResult(List.of());
            MapAnalysisResultView expected = new MapAnalysisResultView(
                    prepared.festivalMapPublicId(),
                    prepared.analysisJobPublicId(),
                    MapAnalysisStatus.COMPLETED,
                    0
            );
            given(mapAnalysisPersistenceService.prepare(
                    festivalId,
                    command,
                    principal
            )).willReturn(prepared);
            given(mapImageAnalyzer.analyze(prepared.analysisRequest()))
                    .willReturn(analysisResult);
            given(mapAnalysisPersistenceService.complete(prepared, analysisResult))
                    .willReturn(expected);

            // when
            MapAnalysisResultView result = mapAnalysisApplicationService.analyzeTestMap(
                    festivalId,
                    command,
                    principal
            );

            // then
            assertThat(result).isEqualTo(expected);
            then(mapAnalysisPersistenceService).should()
                    .complete(prepared, analysisResult);
        }

        @Test
        @DisplayName("외부 분석 실패 시 작업을 실패 상태로 반영한다")
        void fail_AnalyzeTestMap_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            CreateTestMapAnalysisCommand command = command();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            PreparedMapAnalysis prepared = prepared();
            CustomException failure = new CustomException(ErrorCode.INVALID_REQUEST);
            given(mapAnalysisPersistenceService.prepare(
                    festivalId,
                    command,
                    principal
            )).willReturn(prepared);
            given(mapImageAnalyzer.analyze(prepared.analysisRequest()))
                    .willThrow(failure);

            // when & then
            assertThatThrownBy(() -> mapAnalysisApplicationService.analyzeTestMap(
                    festivalId,
                    command,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
            then(mapAnalysisPersistenceService).should().fail(
                    prepared.analysisJobId(),
                    ErrorCode.INVALID_REQUEST.getMessage()
            );
        }
    }

    private CreateTestMapAnalysisCommand command() {
        return new CreateTestMapAnalysisCommand(
                "김밥축제_지적편집도.png",
                "images/김밥축제_지적편집도.png",
                1745,
                1577
        );
    }

    private PreparedMapAnalysis prepared() {
        UUID mapId = UUID.randomUUID();
        return new PreparedMapAnalysis(
                10L,
                mapId,
                20L,
                UUID.randomUUID(),
                new MapImageAnalysisRequest(
                        mapId,
                        "images/김밥축제_지적편집도.png",
                        MapStorageType.TEST_RESOURCE,
                        1745,
                        1577
                )
        );
    }
}
