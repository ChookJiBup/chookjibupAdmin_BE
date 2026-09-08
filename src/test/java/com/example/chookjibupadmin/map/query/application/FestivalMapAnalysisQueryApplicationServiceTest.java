package com.example.chookjibupadmin.map.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisJobService;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJob;
import com.example.chookjibupadmin.map.analysis.domain.MapAnalysisJobStatus;
import com.example.chookjibupadmin.map.command.application.FestivalMapPresentationService;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalMapAnalysisQueryApplicationServiceTest {

    @InjectMocks
    private FestivalMapAnalysisQueryApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService roleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalLocationService festivalLocationService;
    @Mock private FestivalMapService mapService;
    @Mock private MapAnalysisJobService jobService;
    @Mock private FestivalRoadmapService roadmapService;
    @Mock private RoadmapNodeService nodeService;
    @Mock private FestivalMapPresentationService presentationService;
    @Mock private MapPresentationViewAssembler presentationViewAssembler;
    @Mock private MapImageStoragePort storagePort;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private final UUID festivalPublicId = UUID.randomUUID();
    private final UUID mapPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L, "owner@mapo.go.kr"
    );

    @BeforeEach
    void setUp() {
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
        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festivalPublicId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of());
        given(presentationService.findByMapId(10L)).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("이미지 배치도에 저장된 앵커를 편집기 응답에 실어 보낸다")
    void success_Editor_ExposesImageAnchor() {
        FestivalMap map = imageMap();
        map.assignImageAnchor(MapImageAnchor.of(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                new BigDecimal("420.5"),
                new BigDecimal("12.25")
        ));
        stubEditorDependencies(map);

        MapEditorView view = service.editor(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(view.imageAnchor()).isNotNull();
        assertThat(view.imageAnchor().centerLat()).isEqualByComparingTo("37.5665");
        assertThat(view.imageAnchor().centerLng()).isEqualByComparingTo("126.9780");
        assertThat(view.imageAnchor().groundWidthMeters())
                .isEqualByComparingTo("420.50");
        assertThat(view.imageAnchor().rotationDegrees())
                .isEqualByComparingTo("12.250");
    }

    @Test
    @DisplayName("앵커가 없는 이미지 배치도는 앵커를 null로 응답한다")
    void success_Editor_WithoutAnchor() {
        FestivalMap map = imageMap();
        stubEditorDependencies(map);

        MapEditorView view = service.editor(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(view.imageAnchor()).isNull();
    }

    @Test
    @DisplayName("좌표 전용 지도는 얹을 이미지가 없어 앵커를 null로 응답한다")
    void success_Editor_CoordinateMapHasNoAnchor() {
        FestivalMap map = FestivalMap.coordinateOnly(
                20L, 30L, FestivalMapName.of("본행사 배치"), 1L
        );
        ReflectionTestUtils.setField(map, "publicId", mapPublicId);
        ReflectionTestUtils.setField(map, "id", 10L);
        FestivalRoadmap roadmap = roadmap();
        given(mapService.getByPublicId(mapPublicId)).willReturn(map);
        given(roadmapService.getByFestivalId(20L)).willReturn(roadmap);
        given(nodeService.findAll(40L, 10L)).willReturn(List.of());

        MapEditorView view = service.editor(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(view.imageAnchor()).isNull();
        assertThat(view.displayImageUrl()).isNull();
    }

    private void stubEditorDependencies(FestivalMap map) {
        FestivalRoadmap roadmap = roadmap();
        MapAnalysisJob job = Mockito.mock(MapAnalysisJob.class);
        given(job.getStatus()).willReturn(MapAnalysisJobStatus.COMPLETED);
        given(mapService.getByPublicId(mapPublicId)).willReturn(map);
        given(roadmapService.getByFestivalId(20L)).willReturn(roadmap);
        given(nodeService.findAll(40L, 10L)).willReturn(List.of());
        given(jobService.getLatestByMapId(10L)).willReturn(job);
        given(storagePort.createReadUrl("display-key")).willReturn(
                new MapImageReadUrl(
                        URI.create("https://example.com/display.png"),
                        Instant.parse("2026-09-08T00:00:00Z")
                )
        );
    }

    private FestivalRoadmap roadmap() {
        FestivalRoadmap roadmap = FestivalRoadmap.create(20L, 10L, 1L);
        ReflectionTestUtils.setField(roadmap, "id", 40L);
        return roadmap;
    }

    private FestivalMap imageMap() {
        FestivalMap map = FestivalMap.uploaded(
                mapPublicId, 20L, FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
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
        ReflectionTestUtils.setField(map, "id", 10L);
        return map;
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
