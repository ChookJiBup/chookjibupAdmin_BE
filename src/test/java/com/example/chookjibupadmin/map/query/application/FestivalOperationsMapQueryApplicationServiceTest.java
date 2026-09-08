package com.example.chookjibupadmin.map.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.map.command.application.FestivalMapPresentationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.query.application.dto.FestivalOperationsMapView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalOperationsMapQueryApplicationServiceTest {

    @InjectMocks
    private FestivalOperationsMapQueryApplicationService service;

    @Mock private FestivalOperationAccessService festivalOperationAccessService;
    @Mock private FestivalMapService mapService;
    @Mock private FestivalRoadmapService roadmapService;
    @Mock private RoadmapNodeService nodeService;
    @Mock private FestivalMapPresentationService presentationService;
    @Mock private MapPresentationViewAssembler presentationViewAssembler;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private final UUID festivalPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");

    private FestivalMap map;

    @BeforeEach
    void setUp() {
        map = FestivalMap.coordinateOnly(
                20L, 30L, FestivalMapName.of("본행사 배치"), 1L
        );
        ReflectionTestUtils.setField(map, "id", 10L);
        FestivalRoadmap roadmap = FestivalRoadmap.create(20L, 10L, 1L);
        ReflectionTestUtils.setField(roadmap, "id", 40L);

        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, principal))
                .willReturn(20L);
        given(roadmapService.getByFestivalId(20L)).willReturn(roadmap);
        given(mapService.getById(10L)).willReturn(map);
        given(presentationService.findByMapId(10L)).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("승인 부스 마커에 노드 유형을 실어 보낸다")
    void success_BoothMarkerCarriesNodeType() {
        RoadmapNode boothNode = confirmedNode(
                NodeType.BOOTH, "떡볶이 부스", "37.5665", "126.9780"
        );
        ReflectionTestUtils.setField(boothNode, "relatedBoothId", 500L);
        given(nodeService.findAll(40L, 10L)).willReturn(List.of(boothNode));

        FestivalOperationsMapView view = service.getMap(festivalPublicId, principal);

        assertThat(view.booths()).hasSize(1);
        assertThat(view.booths().getFirst().nodeType()).isEqualTo("BOOTH");
        assertThat(view.booths().getFirst().boothId()).isEqualTo(500L);
        assertThat(view.booths().getFirst().lat()).isEqualByComparingTo("37.5665");
        assertThat(view.facilities()).isEmpty();
    }

    @Test
    @DisplayName("부스가 아닌 확정 POINT 노드는 시설 마커로 따로 내려준다")
    void success_FacilityMarkersAreSeparated() {
        RoadmapNode restroom = confirmedNode(
                NodeType.RESTROOM, "제1화장실", "37.5670", "126.9781"
        );
        RoadmapNode entrance = confirmedNode(
                NodeType.ENTRANCE, "정문", "37.5671", "126.9782"
        );
        RoadmapNode boothNode = confirmedNode(
                NodeType.BOOTH, "떡볶이 부스", "37.5665", "126.9780"
        );
        ReflectionTestUtils.setField(boothNode, "relatedBoothId", 500L);
        given(nodeService.findAll(40L, 10L))
                .willReturn(List.of(boothNode, restroom, entrance));

        FestivalOperationsMapView view = service.getMap(festivalPublicId, principal);

        assertThat(view.booths()).hasSize(1);
        assertThat(view.facilities())
                .extracting("name", "nodeType")
                .containsExactly(
                        tuple("제1화장실", "RESTROOM"),
                        tuple("정문", "ENTRANCE")
                );
    }

    @Test
    @DisplayName("승인 전 부스 후보와 미확정 AI 노드는 시설 마커에서 뺀다")
    void success_ExcludesUnapprovedBoothAndDraftNode() {
        RoadmapNode unapprovedBooth = confirmedNode(
                NodeType.BOOTH, "승인 대기 부스", "37.5666", "126.9783"
        );
        RoadmapNode draftRestroom = RoadmapNode.ai(
                40L, 10L, 90L, NodeType.RESTROOM, "AI가 찾은 화장실",
                GeometryType.POINT, pointJson("37.5672", "126.9784"),
                new BigDecimal("0.9000"), "화장실", 3, "2.0"
        );
        given(nodeService.findAll(40L, 10L))
                .willReturn(List.of(unapprovedBooth, draftRestroom));

        FestivalOperationsMapView view = service.getMap(festivalPublicId, principal);

        assertThat(view.booths()).isEmpty();
        assertThat(view.facilities()).isEmpty();
    }

    @Test
    @DisplayName("POINT가 아닌 노드는 부스에도 시설에도 담지 않는다")
    void success_IgnoresNonPointNodes() {
        RoadmapNode zone = RoadmapNode.admin(
                UUID.randomUUID(), 40L, 10L, NodeType.OPEN_SPACE, "A구역",
                GeometryType.POLYGON,
                "{\"points\":[{\"lat\":37.5,\"lng\":127.0}]}",
                1, 1L, "2.0"
        );
        given(nodeService.findAll(40L, 10L)).willReturn(List.of(zone));

        FestivalOperationsMapView view = service.getMap(festivalPublicId, principal);

        assertThat(view.booths()).isEmpty();
        assertThat(view.facilities()).isEmpty();
    }

    private RoadmapNode confirmedNode(
            NodeType type,
            String name,
            String lat,
            String lng
    ) {
        return RoadmapNode.admin(
                UUID.randomUUID(),
                40L,
                10L,
                type,
                name,
                GeometryType.POINT,
                pointJson(lat, lng),
                1,
                1L,
                "2.0"
        );
    }

    private String pointJson(String lat, String lng) {
        return "{\"lat\":" + lat + ",\"lng\":" + lng + "}";
    }
}
