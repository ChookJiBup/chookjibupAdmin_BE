package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.map.analysis.application.MapAnalysisJobService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalMapPurgeServiceTest {

    @InjectMocks
    private FestivalMapPurgeService service;

    @Mock private FestivalMapService festivalMapService;
    @Mock private MapAnalysisJobService mapAnalysisJobService;
    @Mock private FestivalRoadmapService festivalRoadmapService;
    @Mock private RoadmapNodeService roadmapNodeService;
    @Mock private FestivalMap festivalMap;
    @Mock private FestivalRoadmap roadmap;

    @Test
    @DisplayName("분석 작업을 취소하고 배치도 파일 키를 삭제 대상으로 반환한다")
    void success_BeginDeletion() {
        given(festivalMapService.getAllByFestivalIdForUpdate(20L))
                .willReturn(List.of(festivalMap));
        given(festivalMap.getId()).willReturn(30L);
        given(festivalMap.getOriginalImageKey())
                .willReturn(MapImageObjectKey.of("original-key"));
        given(festivalMap.getDisplayImageKey())
                .willReturn(MapImageObjectKey.of("display-key"));
        given(festivalMap.getAnalysisImageKey())
                .willReturn(MapImageObjectKey.of("analysis-key"));

        List<String> objectKeys = service.beginDeletion(20L);

        assertThat(objectKeys).containsExactly(
                "original-key",
                "display-key",
                "analysis-key"
        );
        then(mapAnalysisJobService).should().cancelActive(30L);
        then(festivalMap).should().beginDeletion();
    }

    @Test
    @DisplayName("노드와 로드맵을 먼저 지운 뒤 분석 작업과 배치도를 삭제한다")
    void success_PurgeDatabase_InReferenceOrder() {
        given(festivalRoadmapService.findByFestivalId(20L))
                .willReturn(Optional.of(roadmap));
        given(roadmap.getId()).willReturn(40L);
        given(festivalMapService.getAllByFestivalIdForUpdate(20L))
                .willReturn(List.of(festivalMap));
        given(festivalMap.getId()).willReturn(30L);

        service.purgeDatabase(20L);

        InOrder inOrder = Mockito.inOrder(
                roadmapNodeService,
                festivalRoadmapService,
                mapAnalysisJobService,
                festivalMapService
        );
        inOrder.verify(roadmapNodeService).deleteAllByRoadmapId(40L);
        inOrder.verify(festivalRoadmapService).delete(roadmap);
        inOrder.verify(mapAnalysisJobService).deleteAllByMapIds(List.of(30L));
        inOrder.verify(festivalMapService).deleteAll(List.of(festivalMap));
    }
}
