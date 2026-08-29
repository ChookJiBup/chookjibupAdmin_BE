package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobRepository;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.query.application.FestivalMapAnalysisQueryApplicationService;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** 체크리스트 §9 카카오 좌표 부스맵 E2E(서버 통합) 검증. */
@SpringBootTest
@Transactional
class CoordinateMapFlowIntegrationTest {

    @Autowired private AdminAccountService adminAccountService;
    @Autowired private FestivalApplicationService festivalApplicationService;
    @Autowired private FestivalLocationService festivalLocationService;
    @Autowired private FestivalMapCoordinateRegistrationApplicationService coordinateRegistrationService;
    @Autowired private FestivalMapService mapService;
    @Autowired private FestivalRoadmapService roadmapService;
    @Autowired private RoadmapDraftApplicationService roadmapDraftService;
    @Autowired private FestivalMapAnalysisQueryApplicationService editorQueryService;
    @Autowired private RoadmapNodeService nodeService;
    @Autowired private MapAnalysisJobRepository analysisJobRepository;
    @Autowired private ObjectMapper objectMapper;

    private AdminAccount admin;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        admin = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("coord-e2e@mapo.go.kr"),
                AdminName.of("좌표검증"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        principal = new AdminPrincipal(admin.getId(), admin.getEmailValue());
    }

    @Test
    @DisplayName("JSON 축제 → 좌표 map → 핀 2개 저장 → editor 재조회 → schema 2.0·analysis job 0건")
    void success_CoordinateMapHappyPath() {
        Festival festival = festivalApplicationService.create(
                createFestivalCommand(new BigDecimal("37.5665"), new BigDecimal("126.9780")),
                principal
        );

        var mapView = coordinateRegistrationService.ensureCoordinateMap(
                festival.getPublicId(), "본행사 배치", principal
        );
        FestivalMap map = mapService.getByPublicId(mapView.mapId());
        assertThat(map.isCoordinateMap()).isTrue();
        assertThat(analysisJobRepository.findLatestByMapId(map.getId())).isEmpty();

        var editorBefore = editorQueryService.editor(
                festival.getPublicId(), map.getPublicId(), principal
        );
        assertThat(editorBefore.displayImageUrl()).isNull();
        assertThat(editorBefore.center()).isNotNull();
        assertThat(editorBefore.analysis()).isNull();

        roadmapDraftService.save(
                festival.getPublicId(),
                map.getPublicId(),
                new SaveRoadmapDraftCommand(
                        mapView.editRevision(),
                        List.of(
                                pointChange(null, "CU편의점", 37.5665, 126.9780, 0),
                                pointChange(null, "김밥공장", 37.5675, 126.9790, 1)
                        )
                ),
                principal
        );

        var editorAfter = editorQueryService.editor(
                festival.getPublicId(), map.getPublicId(), principal
        );
        assertThat(editorAfter.nodes()).hasSize(2);
        assertThat(editorAfter.nodes().getFirst().geometrySchemaVersion()).isEqualTo("2.0");

        var roadmap = roadmapService.getByFestivalId(festival.getId());
        List<RoadmapNode> stored = nodeService.findAll(roadmap.getId(), map.getId());
        assertThat(stored).hasSize(2);
        assertThat(stored.getFirst().getGeometrySchemaVersion()).isEqualTo("2.0");
        assertThat(stored.getFirst().getGeometryData()).contains("\"lat\"");
        assertThat(stored.getFirst().getGeometryData()).contains("\"lng\"");
        assertThat(analysisJobRepository.findLatestByMapId(map.getId())).isEmpty();

        var reused = coordinateRegistrationService.ensureCoordinateMap(
                festival.getPublicId(), "다른 이름", principal
        );
        assertThat(reused.mapId()).isEqualTo(mapView.mapId());
    }

    @Test
    @DisplayName("primary lat/lng 없으면 POST maps가 FESTIVAL_MAP_LOCATION_REQUIRED")
    void fail_CreateMapWithoutCoordinates() {
        Festival festival = festivalApplicationService.create(
                createFestivalCommand(new BigDecimal("37.5665"), new BigDecimal("126.9780")),
                principal
        );
        List<FestivalLocation> locations = festivalLocationService.findAllByFestivalId(festival.getId());
        FestivalLocation primary = locations.stream()
                .filter(FestivalLocation::isPrimary)
                .findFirst()
                .orElseThrow();
        ReflectionTestUtils.setField(primary, "latitude", null);
        ReflectionTestUtils.setField(primary, "longitude", null);
        festivalLocationService.saveAll(List.of(primary));

        assertThatThrownBy(() -> coordinateRegistrationService.ensureCoordinateMap(
                festival.getPublicId(), "본행사 배치", principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED)
        );
    }

    @Test
    @DisplayName("lat 999는 ROADMAP_NODE_INVALID, revision 불일치는 ROADMAP_REVISION_CONFLICT")
    void fail_InvalidNodeAndRevisionConflict() {
        Festival festival = festivalApplicationService.create(
                createFestivalCommand(new BigDecimal("37.5665"), new BigDecimal("126.9780")),
                principal
        );
        var mapView = coordinateRegistrationService.ensureCoordinateMap(
                festival.getPublicId(), "본행사 배치", principal
        );
        FestivalMap map = mapService.getByPublicId(mapView.mapId());

        assertThatThrownBy(() -> roadmapDraftService.save(
                festival.getPublicId(),
                map.getPublicId(),
                new SaveRoadmapDraftCommand(
                        mapView.editRevision(),
                        List.of(pointChange(null, "잘못된 핀", 999, 126.9780, 0))
                ),
                principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROADMAP_NODE_INVALID)
        );

        var saved = roadmapDraftService.save(
                festival.getPublicId(),
                map.getPublicId(),
                new SaveRoadmapDraftCommand(
                        mapView.editRevision(),
                        List.of(pointChange(null, "정상 핀", 37.5665, 126.9780, 0))
                ),
                principal
        );

        assertThatThrownBy(() -> roadmapDraftService.save(
                festival.getPublicId(),
                map.getPublicId(),
                new SaveRoadmapDraftCommand(
                        mapView.editRevision(),
                        List.of(pointChange(null, "충돌 핀", 37.5666, 126.9781, 0))
                ),
                principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_REVISION_CONFLICT)
        );

        assertThat(saved.editRevision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("노드 0개 PUT은 ROADMAP_NODE_INVALID")
    void fail_SaveEmptyNodes() {
        Festival festival = festivalApplicationService.create(
                createFestivalCommand(new BigDecimal("37.5665"), new BigDecimal("126.9780")),
                principal
        );
        var mapView = coordinateRegistrationService.ensureCoordinateMap(
                festival.getPublicId(), "본행사 배치", principal
        );
        FestivalMap map = mapService.getByPublicId(mapView.mapId());

        assertThatThrownBy(() -> roadmapDraftService.save(
                festival.getPublicId(),
                map.getPublicId(),
                new SaveRoadmapDraftCommand(mapView.editRevision(), List.of()),
                principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROADMAP_NODE_INVALID)
        );
    }

    private RoadmapNodeChangeCommand pointChange(
            UUID nodeId,
            String name,
            double lat,
            double lng,
            int sortOrder
    ) {
        return new RoadmapNodeChangeCommand(
                nodeId,
                NodeType.BOOTH,
                name,
                GeometryType.POINT,
                objectMapper.valueToTree(java.util.Map.of("lat", lat, "lng", lng)),
                false,
                sortOrder
        );
    }

    private CreateFestivalCommand createFestivalCommand(BigDecimal lat, BigDecimal lng) {
        return new CreateFestivalCommand(
                null,
                "좌표 E2E 축제",
                "검증용",
                List.of(new FestivalLocationCommand(
                        FestivalLocationType.MAIN_VENUE,
                        "본행사장",
                        "서울특별시 마포구",
                        null,
                        null,
                        null,
                        null,
                        lat,
                        lng,
                        true,
                        0
                )),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                LocalTime.of(10, 0),
                LocalTime.of(20, 0)
        );
    }
}
