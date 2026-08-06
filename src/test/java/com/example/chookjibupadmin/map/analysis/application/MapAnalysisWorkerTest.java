package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;
import com.example.chookjibupadmin.map.analysis.application.port.MapBlueprintAnalysisPort;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisWorkerTest {

    private MapAnalysisWorker worker;

    @Mock
    private MapAnalysisJobService jobService;

    @Mock
    private FestivalMapService mapService;

    @Mock
    private MapImageStoragePort storagePort;

    @Mock
    private MapBlueprintAnalysisPort analysisPort;

    @Mock
    private MapAnalysisResultApplicationService resultService;

    @Mock
    private FestivalMap festivalMap;

    private final MapAnalysisProperties properties =
            new MapAnalysisProperties(
                    "openai",
                    null,
                    "test-key",
                    "gpt-5.6",
                    null,
                    null,
                    3,
                    3000,
                    1024
            );

    private MapAnalysisJob job;

    @BeforeEach
    void setUp() {
        worker = new MapAnalysisWorker(
                jobService,
                mapService,
                storagePort,
                analysisPort,
                resultService,
                properties
        );
        job = MapAnalysisJob.pending(
                10L,
                "openai",
                "gpt-5.6",
                "analysis-key",
                "a".repeat(64),
                1200,
                800
        );
        job.start();
    }

    @Test
    @DisplayName("이미지를 읽고 OpenAI 분석 결과를 반영 서비스에 전달한다")
    void success_Process() {
        // given
        byte[] image = new byte[]{1, 2, 3};
        MapAnalysisResult result = new MapAnalysisResult(List.of());
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(festivalMap.getAnalysisContentType())
                .willReturn(MapImageContentType.of("image/jpeg"));
        given(storagePort.read("analysis-key", 1024))
                .willReturn(image);
        given(analysisPort.analyze(image, "image/jpeg", 1200, 800))
                .willReturn(result);

        // when
        worker.process(job);

        // then
        then(resultService).should().complete(job.getPublicId(), result);
        then(jobService).should(never()).save(job);
    }

    @Test
    @DisplayName("재시도 가능한 외부 오류는 최대 시도 전까지 대기 상태로 되돌린다")
    void success_Process_RetryableFailure() {
        // given
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(storagePort.read("analysis-key", 1024))
                .willThrow(new MapAnalysisException(
                        "OPENAI_HTTP_429",
                        "OpenAI request failed",
                        true
                ));

        // when
        worker.process(job);

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.PENDING);
        assertThat(job.getFailureCode()).isEqualTo("OPENAI_HTTP_429");
        then(jobService).should().save(job);
        then(resultService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("재시도 불가능 오류는 즉시 실패 상태로 종료한다")
    void success_Process_NonRetryableFailure() {
        // given
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(storagePort.read("analysis-key", 1024))
                .willThrow(new MapAnalysisException(
                        "OPENAI_REFUSAL",
                        "OpenAI refused map analysis",
                        false
                ));

        // when
        worker.process(job);

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.FAILED);
        assertThat(job.getFailureCode()).isEqualTo("OPENAI_REFUSAL");
        then(jobService).should().save(job);
        then(resultService).shouldHaveNoInteractions();
    }
}
