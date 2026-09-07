package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.analysis.application.dto.MapAnalysisResult;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MapAnalysisResultApplicationServiceTest {

    @Mock
    private MapAnalysisJobService jobService;

    @Mock
    private FestivalMapService mapService;

    @Mock
    private FestivalRoadmapService roadmapService;

    @Mock
    private RoadmapNodeService nodeService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MapAnalysisResultApplicationService resultService;
    private FestivalMap festivalMap;
    private FestivalRoadmap roadmap;

    @BeforeEach
    void setUp() {
        resultService = new MapAnalysisResultApplicationService(
                jobService,
                mapService,
                roadmapService,
                nodeService,
                new MapGeometryValidator(),
                new MapAnchorProjector(),
                objectMapper
        );
        festivalMap = festivalMap();
        roadmap = FestivalRoadmap.create(1L, 10L, 2L);
        ReflectionTestUtils.setField(festivalMap, "id", 10L);
        ReflectionTestUtils.setField(roadmap, "id", 20L);
    }

    @Test
    @DisplayName("현재 도면과 일치하는 유효한 AI 노드를 저장하고 분석을 완료한다")
    void success_Complete() throws Exception {
        // given
        MapAnalysisJob job = processingJob("analysis-key", "c".repeat(64));
        MapAnalysisResult result = new MapAnalysisResult(List.of(
                analyzedNode()
        ));
        given(jobService.getByPublicId(job.getPublicId())).willReturn(job);
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(roadmapService.getByFestivalId(1L)).willReturn(roadmap);

        // when
        resultService.complete(job.getPublicId(), result);

        // then
        then(nodeService).should().saveAll(
                argThat(nodes ->
                        ((List<?>) nodes).size() == 1
                )
        );
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.COMPLETED);
        assertThat(job.getAcceptedCount()).isEqualTo(1);
        assertThat(roadmap.getStatus())
                .isEqualTo(RoadmapStatus.REVIEW_REQUIRED);
        then(roadmapService).should().save(roadmap);
        then(jobService).should().save(job);
    }

    @Test
    @DisplayName("작업 체크섬과 현재 도면이 다르면 늦게 도착한 결과를 취소한다")
    void success_Complete_StaleAnalysisSourceCancelled() throws Exception {
        // given
        MapAnalysisJob job = processingJob("analysis-key", "d".repeat(64));
        given(jobService.getByPublicId(job.getPublicId())).willReturn(job);
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(roadmapService.getByFestivalId(1L)).willReturn(roadmap);

        // when
        resultService.complete(
                job.getPublicId(),
                new MapAnalysisResult(List.of(analyzedNode()))
        );

        // then
        assertThat(job.getStatus()).isEqualTo(MapAnalysisJobStatus.CANCELLED);
        then(jobService).should().save(job);
        then(nodeService).should(never()).saveAll(any());
        // 취소된 작업이 로드맵을 ANALYZING에 가둔 채 두지 않는다.
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.EDITING);
        then(roadmapService).should().save(roadmap);
    }

    @Test
    @DisplayName("앵커가 없는 도면은 AI가 준 이미지 좌표(1.0)를 그대로 저장한다")
    void success_Complete_KeepsImageGeometryWithoutAnchor() throws Exception {
        // given
        MapAnalysisJob job = processingJob("analysis-key", "c".repeat(64));
        given(jobService.getByPublicId(job.getPublicId())).willReturn(job);
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(roadmapService.getByFestivalId(1L)).willReturn(roadmap);

        // when
        resultService.complete(
                job.getPublicId(),
                new MapAnalysisResult(List.of(analyzedNode()))
        );

        // then
        then(nodeService).should().saveAll(argThat(nodes -> {
            RoadmapNode node = (RoadmapNode) ((List<?>) nodes).get(0);
            return "1.0".equals(node.getGeometrySchemaVersion())
                    && node.getGeometryType() == GeometryType.RECTANGLE;
        }));
    }

    @Test
    @DisplayName("앵커가 있는 도면은 AI가 준 영역 도형을 위경도 POINT(2.0)로 옮겨 저장한다")
    void success_Complete_ProjectsToWgs84PointWithAnchor() throws Exception {
        // given
        festivalMap.assignImageAnchor(MapImageAnchor.of(
                new BigDecimal("37.0000000"),
                new BigDecimal("127.0000000"),
                new BigDecimal("300.00"),
                new BigDecimal("0.000")
        ));
        MapAnalysisJob job = processingJob("analysis-key", "c".repeat(64));
        given(jobService.getByPublicId(job.getPublicId())).willReturn(job);
        given(mapService.getById(10L)).willReturn(festivalMap);
        given(roadmapService.getByFestivalId(1L)).willReturn(roadmap);

        // when
        resultService.complete(
                job.getPublicId(),
                new MapAnalysisResult(List.of(analyzedNode()))
        );

        // then
        then(nodeService).should().saveAll(argThat(nodes -> {
            RoadmapNode node = (RoadmapNode) ((List<?>) nodes).get(0);
            if (!"2.0".equals(node.getGeometrySchemaVersion())
                    || node.getGeometryType() != GeometryType.POINT) {
                return false;
            }
            try {
                var geometry = objectMapper.readTree(node.getGeometryData());
                // RECTANGLE(0.1,0.2,0.3x0.2)의 중심은 정규화 (0.25, 0.3)이다.
                return geometry.has("lat") && geometry.has("lng")
                        && geometry.get("lat").doubleValue() > 37.0
                        && geometry.get("lng").doubleValue() < 127.0;
            } catch (Exception exception) {
                return false;
            }
        }));
        assertThat(job.getAcceptedCount()).isEqualTo(1);
    }

    private MapAnalysisJob processingJob(String key, String checksum) {
        MapAnalysisJob job = MapAnalysisJob.pending(
                10L,
                "openai",
                "gpt-5.6",
                key,
                checksum,
                1200,
                800
        );
        ReflectionTestUtils.setField(job, "id", 30L);
        job.start();
        return job;
    }

    private AnalyzedMapNode analyzedNode() throws Exception {
        return new AnalyzedMapNode(
                NodeType.BOOTH,
                "부스 1",
                GeometryType.RECTANGLE,
                objectMapper.readTree("""
                        {
                          "x": 0.1,
                          "y": 0.2,
                          "width": 0.3,
                          "height": 0.2,
                          "rotation": 0
                        }
                        """),
                new BigDecimal("0.95"),
                "부스 1"
        );
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                FestivalMapName.of("축제 도면"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L),
                MapImageFileSize.of(90L),
                MapImageFileSize.of(80L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)),
                2L
        );
    }
}
