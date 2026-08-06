package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisQueueApplicationServiceTest {

    @Mock
    private MapAnalysisJobService jobService;

    @Mock
    private FestivalRoadmapService roadmapService;

    @Mock
    private FestivalMap festivalMap;

    private MapAnalysisQueueApplicationService queueService;

    @BeforeEach
    void setUp() {
        MapAnalysisProperties properties = new MapAnalysisProperties(
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
        queueService = new MapAnalysisQueueApplicationService(
                jobService,
                roadmapService,
                properties
        );
        given(jobService.save(any(MapAnalysisJob.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("최초 도면 등록 시 로드맵과 분석 대기 작업을 함께 생성한다")
    void success_EnqueueInitial() {
        // given
        configureMap(festivalMap, 10L);
        given(festivalMap.getCreatedByAdminId()).willReturn(2L);

        // when
        MapAnalysisJob job = queueService.enqueueInitial(festivalMap);

        // then
        assertThat(job.getMapId()).isEqualTo(10L);
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.PENDING);
        assertThat(job.getInputImageKey()).isEqualTo("analysis-key");
        then(roadmapService).should().save(
                argThat(roadmap ->
                        roadmap.getFestivalId().equals(1L)
                                && roadmap.getCurrentMapId().equals(10L)
                                && roadmap.getStatus() == RoadmapStatus.ANALYZING
                )
        );
    }

    @Test
    @DisplayName("도면 교체 시 이전 작업을 취소하고 로드맵의 현재 도면을 변경한다")
    void success_EnqueueReplacement() {
        // given
        FestivalMap previous = mock(FestivalMap.class);
        FestivalMap replacement = mock(FestivalMap.class);
        given(previous.getId()).willReturn(10L);
        configureMap(replacement, 11L);
        FestivalRoadmap roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        given(roadmapService.getByFestivalId(1L)).willReturn(roadmap);

        // when
        MapAnalysisJob job = queueService.enqueueReplacement(
                previous,
                replacement
        );

        // then
        then(jobService).should().cancelActive(10L);
        assertThat(roadmap.getCurrentMapId()).isEqualTo(11L);
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.ANALYZING);
        assertThat(job.getMapId()).isEqualTo(11L);
    }

    private void configureMap(FestivalMap map, Long mapId) {
        given(map.getId()).willReturn(mapId);
        given(map.getFestivalId()).willReturn(1L);
        given(map.getAnalysisImageKey())
                .willReturn(MapImageObjectKey.of("analysis-key"));
        given(map.getAnalysisChecksumSha256())
                .willReturn(Sha256Checksum.of("a".repeat(64)));
        given(map.getAnalysisImageDimensions())
                .willReturn(MapImageDimensions.of(1200, 800));
    }
}
