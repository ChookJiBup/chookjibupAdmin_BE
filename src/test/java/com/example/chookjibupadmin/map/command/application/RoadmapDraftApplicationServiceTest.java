package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.application.MapGeometryValidator;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapNodeChangeCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.dto.RoadmapZoneCommand;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoadmapDraftApplicationServiceTest {

    @InjectMocks
    private RoadmapDraftApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService roleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalMapService mapService;
    @Mock private FestivalRoadmapService roadmapService;
    @Mock private RoadmapNodeService nodeService;

    private final MapGeometryValidator geometryValidator =
            new MapGeometryValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID festivalPublicId = UUID.randomUUID();
    private final UUID mapPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L, "owner@mapo.go.kr"
    );
    private FestivalRoadmap roadmap;
    private RoadmapNode existingNode;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "geometryValidator", geometryValidator);
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);

        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        Festival festival = festival();
        ReflectionTestUtils.setField(festival, "id", 20L);
        FestivalMap map = festivalMap();
        ReflectionTestUtils.setField(map, "id", 10L);
        roadmap = FestivalRoadmap.create(20L, 10L, 1L);
        ReflectionTestUtils.setField(roadmap, "id", 30L);
        roadmap.analysisCompleted();
        existingNode = RoadmapNode.ai(
                30L, 10L, 40L, NodeType.BOOTH, "기존 부스",
                GeometryType.RECTANGLE,
                "{\"x\":0.1,\"y\":0.1,\"width\":0.1,\"height\":0.1,\"rotation\":0}",
                new BigDecimal("0.9000"), "기존 부스", 0
        );

        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festivalPublicId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
        given(mapService.getByPublicId(mapPublicId)).willReturn(map);
        given(roadmapService.getByFestivalIdForUpdate(20L)).willReturn(roadmap);
        given(nodeService.findAll(30L, 10L)).willReturn(List.of(existingNode));
    }

    @Test
    @DisplayName("신규 부스의 clientNodeId를 구역 멤버십으로 함께 저장한다")
    void success_Save_ZonesWithNewBooth() {
        UUID clientNodeId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        RoadmapNodeChangeCommand newBooth = new RoadmapNodeChangeCommand(
                null, clientNodeId, NodeType.BOOTH, "신규 부스",
                GeometryType.RECTANGLE,
                objectMapper.valueToTree(java.util.Map.of(
                        "x", 0.2, "y", 0.2, "width", 0.1,
                        "height", 0.1, "rotation", 0
                )), false, 1
        );

        service.save(festivalPublicId, mapPublicId,
                new SaveRoadmapDraftCommand(1L, List.of(newBooth), List.of(
                        new RoadmapZoneCommand(zoneId, "판매 구역", 0, List.of(clientNodeId))
                )), principal);

        assertThat(roadmap.getZones()).hasSize(1);
        assertThat(roadmap.getZones().getFirst().zoneId()).isEqualTo(zoneId);
        assertThat(roadmap.getZones().getFirst().boothNodeIds()).containsExactly(clientNodeId);
    }

    @Test
    @DisplayName("신규 생성과 기존 수정 및 삭제를 한 리비전으로 일괄 저장한다")
    void success_Save() {
        RoadmapNode deletedNode = RoadmapNode.ai(
                30L, 10L, 40L, NodeType.STAGE, "삭제 무대",
                GeometryType.POINT, "{\"x\":0.2,\"y\":0.2}",
                new BigDecimal("0.8000"), "무대", 1
        );
        given(nodeService.findAll(30L, 10L))
                .willReturn(List.of(existingNode, deletedNode));

        var result = service.save(
                festivalPublicId,
                mapPublicId,
                new SaveRoadmapDraftCommand(1L, List.of(
                        change(existingNode.getPublicId(), "수정 부스", false, 0),
                        change(null, "신규 부스", false, 1),
                        new RoadmapNodeChangeCommand(
                                deletedNode.getPublicId(), null, null,
                                null, null, true, null
                        )
                )),
                principal
        );

        assertThat(result.editRevision()).isEqualTo(2L);
        assertThat(existingNode.getNodeName()).isEqualTo("수정 부스");
        ArgumentCaptor<Iterable<RoadmapNode>> saved =
                ArgumentCaptor.forClass(Iterable.class);
        then(nodeService).should().saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        then(nodeService).should().deleteAll(List.of(deletedNode));
    }

    @Test
    @DisplayName("기준 리비전이 오래되면 노드를 저장하지 않는다")
    void fail_Save_RevisionConflict() {
        assertThatThrownBy(() -> service.save(
                festivalPublicId,
                mapPublicId,
                new SaveRoadmapDraftCommand(
                        0L,
                        List.of(change(existingNode.getPublicId(), "수정", false, 0))
                ),
                principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_REVISION_CONFLICT)
        );

        then(nodeService).should(never()).saveAll(any());
        then(nodeService).should(never()).deleteAll(any());
    }

    @Test
    @DisplayName("이미지 경계를 벗어난 노드가 하나라도 있으면 전체 저장을 거절한다")
    void fail_Save_InvalidGeometry() {
        RoadmapNodeChangeCommand invalid = new RoadmapNodeChangeCommand(
                existingNode.getPublicId(), NodeType.BOOTH, "부스",
                GeometryType.RECTANGLE,
                objectMapper.valueToTree(java.util.Map.of(
                        "x", 0.9, "y", 0.1, "width", 0.2,
                        "height", 0.1, "rotation", 0
                )),
                false,
                0
        );

        assertThatThrownBy(() -> service.save(
                festivalPublicId,
                mapPublicId,
                new SaveRoadmapDraftCommand(1L, List.of(invalid)),
                principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_NODE_INVALID)
        );

        then(nodeService).should(never()).saveAll(any());
    }

    private RoadmapNodeChangeCommand change(
            UUID nodeId,
            String name,
            boolean deleted,
            int sortOrder
    ) {
        return new RoadmapNodeChangeCommand(
                nodeId,
                NodeType.BOOTH,
                name,
                GeometryType.RECTANGLE,
                objectMapper.valueToTree(java.util.Map.of(
                        "x", 0.1, "y", 0.1, "width", 0.1,
                        "height", 0.1, "rotation", 0
                )),
                deleted,
                sortOrder
        );
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                mapPublicId, 20L, FestivalMapName.of("배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(1), MapImageFileSize.of(1),
                MapImageFileSize.of(1),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)), 1L
        );
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(20, 0)
                )
        );
    }
}
