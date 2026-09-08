package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import com.example.chookjibupadmin.map.command.domain.FestivalMapStorageStatus;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalMapLifecycleApplicationServiceTest {

    @InjectMocks
    private FestivalMapLifecycleApplicationService service;

    @Mock
    private FestivalMapService festivalMapService;
    @Mock
    private FestivalService festivalService;
    @Mock
    private FestivalLocationService festivalLocationService;
    @Mock
    private FestivalRoadmapService festivalRoadmapService;
    @Mock
    private RoadmapNodeService roadmapNodeService;
    @Mock
    private MapAnalysisQueueApplicationService mapAnalysisQueueService;

    @Test
    @DisplayName("축제 행을 잠근 뒤 현재 배치도를 새 배치도로 교체한다")
    void success_Replace_WithFestivalLock() {
        UUID currentMapId = UUID.randomUUID();
        FestivalMap current = festivalMap(currentMapId, "current-original", "current-display");
        FestivalMap replacement = festivalMap(
                UUID.randomUUID(), "new-original", "new-display"
        );
        ReflectionTestUtils.setField(current, "id", 10L);
        current.assignLocation(30L);
        given(festivalRoadmapService.findByFestivalId(20L)).willReturn(Optional.empty());
        given(festivalLocationService.findAllByFestivalId(20L)).willReturn(List.of());
        given(festivalMapService.getByPublicIdForUpdate(currentMapId))
                .willReturn(current);
        given(festivalMapService.save(replacement)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(replacement, "id", 11L);
            return replacement;
        });

        FestivalMap result = service.replace(currentMapId, 20L, replacement);

        assertThat(result).isSameAs(replacement);
        assertThat(current.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.REPLACED);
        assertThat(replacement.getReplacesMapId()).isEqualTo(10L);
        assertThat(replacement.getLocationId()).isEqualTo(30L);
        InOrder order = inOrder(festivalService, festivalMapService);
        order.verify(festivalService).getByIdForUpdate(20L);
        order.verify(festivalMapService).getByPublicIdForUpdate(currentMapId);
        org.mockito.Mockito.verify(mapAnalysisQueueService)
                .enqueueReplacement(current, replacement);
    }

    @Test
    @DisplayName("좌표 전용 지도도 배치도 이미지로 교체할 수 있고 기본 앵커가 붙는다")
    void success_Replace_CoordinateMapWithDefaultAnchor() {
        UUID currentMapId = UUID.randomUUID();
        FestivalMap current = FestivalMap.coordinateOnly(
                20L, 30L, FestivalMapName.of("본행사 배치"), 1L
        );
        ReflectionTestUtils.setField(current, "publicId", currentMapId);
        ReflectionTestUtils.setField(current, "id", 10L);
        FestivalMap replacement = festivalMap(
                UUID.randomUUID(), "new-original", "new-display"
        );
        // 중첩 스텁은 Mockito가 미완성 스텁으로 오인하므로 먼저 만들어 둔다.
        FestivalLocation primary = primaryLocation();
        given(festivalRoadmapService.findByFestivalId(20L)).willReturn(Optional.empty());
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of(primary));
        given(festivalMapService.getByPublicIdForUpdate(currentMapId))
                .willReturn(current);
        given(festivalMapService.save(replacement)).willReturn(replacement);

        FestivalMap result = service.replace(currentMapId, 20L, replacement);

        assertThat(result.hasImageAnchor()).isTrue();
        assertThat(result.geometrySchemaVersion()).isEqualTo("2.0");
        assertThat(result.getImageAnchor().getCenterLatitude())
                .isEqualByComparingTo("37.5");
        assertThat(result.getImageAnchor().getGroundWidthMeters())
                .isEqualByComparingTo("300");
        assertThat(current.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.REPLACED);
    }

    @Test
    @DisplayName("승인된 부스가 연결된 지도는 교체를 막는다")
    void fail_Replace_ApprovedBoothExists() {
        UUID currentMapId = UUID.randomUUID();
        FestivalMap current = festivalMap(currentMapId, "current-original", "current-display");
        ReflectionTestUtils.setField(current, "id", 10L);
        FestivalMap replacement = festivalMap(
                UUID.randomUUID(), "new-original", "new-display"
        );
        FestivalRoadmap roadmap = FestivalRoadmap.create(20L, 10L, 1L);
        ReflectionTestUtils.setField(roadmap, "id", 40L);
        given(festivalMapService.getByPublicIdForUpdate(currentMapId))
                .willReturn(current);
        given(festivalRoadmapService.findByFestivalId(20L))
                .willReturn(Optional.of(roadmap));
        given(roadmapNodeService.findAll(40L, 10L))
                .willReturn(List.of(approvedBoothNode()));

        assertThatThrownBy(() -> service.replace(currentMapId, 20L, replacement))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.FESTIVAL_MAP_REPLACE_BLOCKED_BY_BOOTH
                );
        assertThat(current.getStorageStatus())
                .isEqualTo(FestivalMapStorageStatus.UPLOADED);
    }

    @Test
    @DisplayName("이미지 배치도의 앵커를 관리자가 보정한 값으로 저장한다")
    void success_UpdateImageAnchor() {
        UUID mapId = UUID.randomUUID();
        FestivalMap map = festivalMap(mapId, "original", "display");
        given(festivalMapService.getByPublicIdForUpdate(mapId)).willReturn(map);

        MapImageAnchor saved = service.updateImageAnchor(
                mapId,
                20L,
                MapImageAnchor.of(
                        new BigDecimal("37.5665"),
                        new BigDecimal("126.9780"),
                        new BigDecimal("420.5"),
                        new BigDecimal("12.25")
                )
        );

        assertThat(map.hasImageAnchor()).isTrue();
        assertThat(map.geometrySchemaVersion()).isEqualTo("2.0");
        assertThat(saved.getCenterLatitude()).isEqualByComparingTo("37.5665");
        assertThat(saved.getCenterLongitude()).isEqualByComparingTo("126.9780");
        assertThat(saved.getGroundWidthMeters()).isEqualByComparingTo("420.50");
        assertThat(saved.getRotationDegrees()).isEqualByComparingTo("12.250");
    }

    @Test
    @DisplayName("좌표 전용 지도는 얹을 이미지가 없어 앵커 수정을 막는다")
    void fail_UpdateImageAnchor_CoordinateMap() {
        UUID mapId = UUID.randomUUID();
        FestivalMap map = FestivalMap.coordinateOnly(
                20L, 30L, FestivalMapName.of("본행사 배치"), 1L
        );
        ReflectionTestUtils.setField(map, "publicId", mapId);
        given(festivalMapService.getByPublicIdForUpdate(mapId)).willReturn(map);
        MapImageAnchor anchor = MapImageAnchor.of(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                new BigDecimal("300"),
                BigDecimal.ZERO
        );

        assertThatThrownBy(() -> service.updateImageAnchor(mapId, 20L, anchor))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.FESTIVAL_MAP_INVALID_STATUS
                );
        assertThat(map.hasImageAnchor()).isFalse();
    }

    @Test
    @DisplayName("다른 축제의 지도에는 앵커를 저장하지 않는다")
    void fail_UpdateImageAnchor_OtherFestivalMap() {
        UUID mapId = UUID.randomUUID();
        FestivalMap map = festivalMap(mapId, "original", "display");
        given(festivalMapService.getByPublicIdForUpdate(mapId)).willReturn(map);
        MapImageAnchor anchor = MapImageAnchor.of(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                new BigDecimal("300"),
                BigDecimal.ZERO
        );

        assertThatThrownBy(() -> service.updateImageAnchor(mapId, 99L, anchor))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.FESTIVAL_MAP_NOT_FOUND
                );
        assertThat(map.hasImageAnchor()).isFalse();
    }

    private RoadmapNode approvedBoothNode() {
        RoadmapNode node = RoadmapNode.admin(
                40L, 10L, NodeType.BOOTH, "부스 1", GeometryType.POINT,
                "{\"lat\":37.5,\"lng\":127.0}", 0, 1L, "2.0"
        );
        node.approveBooth(99L, 1L);
        return node;
    }

    private FestivalLocation primaryLocation() {
        FestivalLocation location = org.mockito.Mockito.mock(FestivalLocation.class);
        given(location.isPrimary()).willReturn(true);
        given(location.getLatitude()).willReturn(new BigDecimal("37.5000000"));
        given(location.getLongitude()).willReturn(new BigDecimal("127.0000000"));
        return location;
    }

    private FestivalMap festivalMap(
            UUID publicId,
            String originalKey,
            String displayKey
    ) {
        return FestivalMap.uploaded(
                publicId, 20L, FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of(originalKey),
                MapImageObjectKey.of(displayKey),
                MapImageObjectKey.of(displayKey + "-analysis"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L), MapImageFileSize.of(90L),
                MapImageFileSize.of(80L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)), 1L
        );
    }
}
