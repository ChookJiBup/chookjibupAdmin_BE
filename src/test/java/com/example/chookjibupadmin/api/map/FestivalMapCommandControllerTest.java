package com.example.chookjibupadmin.api.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalMapResponse;
import com.example.chookjibupadmin.api.map.dto.CreateCoordinateMapRequest;
import com.example.chookjibupadmin.api.map.dto.CreateCoordinateMapResponse;
import com.example.chookjibupadmin.api.map.dto.SaveRoadmapDraftRequest;
import com.example.chookjibupadmin.api.map.dto.SaveRoadmapDraftResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.map.command.application.FestivalMapCoordinateRegistrationApplicationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapManagementApplicationService;
import com.example.chookjibupadmin.map.command.application.RoadmapDraftApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.CoordinateMapView;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.SaveRoadmapDraftCommand;
import com.example.chookjibupadmin.map.command.application.dto.SavedRoadmapDraft;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.query.application.dto.MapCenterView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FestivalMapCommandControllerTest {

    @InjectMocks
    private FestivalMapCommandController controller;

    @Mock
    private FestivalMapManagementApplicationService managementService;

    @Mock
    private FestivalMapCoordinateRegistrationApplicationService coordinateRegistrationService;

    @Mock
    private RoadmapDraftApplicationService roadmapDraftService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("좌표 전용 지도 준비 요청을 등록 서비스로 전달한다")
    void success_CreateCoordinateMap() {
        UUID festivalId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        given(coordinateRegistrationService.ensureCoordinateMap(
                festivalId, "본행사 배치", principal
        )).willReturn(new CoordinateMapView(
                mapId,
                "본행사 배치",
                0L,
                "EDITING",
                new MapCenterView(new BigDecimal("37.5665"), new BigDecimal("126.9780"))
        ));

        ApiResponse<CreateCoordinateMapResponse> response = controller.createCoordinateMap(
                festivalId,
                new CreateCoordinateMapRequest("본행사 배치"),
                principal
        );

        assertThat(response.data().mapId()).isEqualTo(mapId);
        assertThat(response.data().center().lat()).isEqualByComparingTo("37.5665");
        then(coordinateRegistrationService).should().ensureCoordinateMap(
                festivalId, "본행사 배치", principal
        );
    }

    @Test
    @DisplayName("지도 편집 요청을 애플리케이션 Command로 변환한다")
    void success_SaveEditor() {
        UUID festivalId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        SaveRoadmapDraftRequest request = new SaveRoadmapDraftRequest(
                3L,
                List.of(new SaveRoadmapDraftRequest.NodeChangeRequest(
                        nodeId,
                        NodeType.BOOTH,
                        "부스 1",
                        GeometryType.POINT,
                        Map.of("x", 0.1, "y", 0.2),
                        false,
                        0
                ))
        );
        given(roadmapDraftService.save(any(), any(), any(), any()))
                .willReturn(new SavedRoadmapDraft(4L));

        ApiResponse<SaveRoadmapDraftResponse> response = controller.saveEditor(
                festivalId,
                mapId,
                request,
                principal
        );

        assertThat(response.data().editRevision()).isEqualTo(4L);
        ArgumentCaptor<SaveRoadmapDraftCommand> captor =
                ArgumentCaptor.forClass(SaveRoadmapDraftCommand.class);
        then(roadmapDraftService).should().save(
                org.mockito.ArgumentMatchers.eq(festivalId),
                org.mockito.ArgumentMatchers.eq(mapId),
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(principal)
        );
        assertThat(captor.getValue().baseRevision()).isEqualTo(3L);
        assertThat(captor.getValue().nodes().getFirst().nodeId())
                .isEqualTo(nodeId);
    }

    @Test
    @DisplayName("교체 이미지 multipart를 프레임워크 독립 Command로 변환한다")
    void success_Replace() throws Exception {
        UUID festivalId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        MockMultipartFile image = new MockMultipartFile(
                "image", "new-map.png", "image/png", new byte[]{1, 2}
        );
        given(managementService.replace(any(), any(), any(), any(), any()))
                .willReturn(festivalMap());

        ApiResponse<CreateFestivalMapResponse> response = controller.replace(
                festivalId, mapId, "새 배치도", image, principal
        );

        assertThat(response.data().mapName()).isEqualTo("새 배치도");
        ArgumentCaptor<MapImageUploadCommand> captor =
                ArgumentCaptor.forClass(MapImageUploadCommand.class);
        then(managementService).should().replace(
                org.mockito.ArgumentMatchers.eq(festivalId),
                org.mockito.ArgumentMatchers.eq(mapId),
                org.mockito.ArgumentMatchers.eq("새 배치도"),
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(principal)
        );
        assertThat(captor.getValue().originalFileName()).isEqualTo("new-map.png");
        assertThat(captor.getValue().fileSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제 요청을 축제와 배치도 식별자로 전달한다")
    void success_Delete() {
        UUID festivalId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");

        controller.delete(festivalId, mapId, principal);

        then(managementService).should().delete(festivalId, mapId, principal);
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                UUID.randomUUID(), 20L, FestivalMapName.of("새 배치도"),
                MapImageFileName.of("new-map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(2), MapImageFileSize.of(2),
                MapImageFileSize.of(2),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)), 1L
        );
    }
}
