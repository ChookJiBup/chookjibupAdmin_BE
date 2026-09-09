package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
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
import com.example.chookjibupadmin.map.command.application.dto.PublishedRoadmap;
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
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoadmapPublishApplicationServiceTest {

    @InjectMocks
    private RoadmapPublishApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService roleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalMapService mapService;
    @Mock private FestivalRoadmapService roadmapService;
    @Mock private RoadmapNodeService nodeService;

    private final UUID festivalPublicId = UUID.randomUUID();
    private final UUID mapPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L, "owner@mapo.go.kr"
    );
    private FestivalRoadmap roadmap;

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
        FestivalMap map = festivalMap();
        ReflectionTestUtils.setField(map, "id", 10L);
        roadmap = FestivalRoadmap.createForCoordinateMap(20L, 10L, 1L);
        ReflectionTestUtils.setField(roadmap, "id", 30L);

        // 권한 단계나 상태 검사에서 거절되는 경우에는 뒤쪽 협력자를 부르지 않으므로 전부 lenient로 둔다.
        lenient().when(adminAccountService.getById(1L)).thenReturn(admin);
        lenient().when(festivalService.getByPublicId(festivalPublicId)).thenReturn(festival);
        lenient().when(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .thenReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
        lenient().when(mapService.getByPublicId(mapPublicId)).thenReturn(map);
        lenient().when(roadmapService.getByFestivalIdForUpdate(20L)).thenReturn(roadmap);
    }

    @Test
    @DisplayName("검수된 부스가 있으면 로드맵을 공개하고 공개 부스 수를 돌려준다")
    void success_Publish() {
        given(nodeService.findAll(30L, 10L)).willReturn(List.of(
                adminBooth("부스 1"),
                adminBooth("부스 2"),
                adminNode(NodeType.STAGE, "무대")
        ));

        PublishedRoadmap published = service.publish(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(published.roadmapStatus()).isEqualTo(RoadmapStatus.PUBLISHED.name());
        assertThat(published.publishedBoothCount()).isEqualTo(2);
        assertThat(published.publishedVersion()).isEqualTo(roadmap.getEditRevision());
        assertThat(roadmap.isPublished()).isTrue();
    }

    @Test
    @DisplayName("부스가 시설뿐이면 방문객에게 보일 게 없어 공개를 거절한다")
    void fail_Publish_NoBooth() {
        given(nodeService.findAll(30L, 10L))
                .willReturn(List.of(adminNode(NodeType.RESTROOM, "화장실")));

        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_PUBLISH_NO_BOOTH)
        );
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("AI가 찾기만 하고 검수하지 않은 부스는 공개 대상으로 세지 않는다")
    void fail_Publish_OnlyUnreviewedAiBooths() {
        given(nodeService.findAll(30L, 10L)).willReturn(List.of(RoadmapNode.ai(
                30L, 10L, 40L, NodeType.BOOTH, "AI 부스",
                GeometryType.POINT, "{\"lat\":37.5665,\"lng\":126.9780}",
                new BigDecimal("0.9000"), "AI 부스", 0, "2.0"
        )));

        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_PUBLISH_NO_BOOTH)
        );
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("분석 중인 로드맵은 공개하지 않는다")
    void fail_Publish_WhileAnalyzing() {
        FestivalRoadmap analyzing = FestivalRoadmap.create(20L, 10L, 1L);
        ReflectionTestUtils.setField(analyzing, "id", 30L);
        given(roadmapService.getByFestivalIdForUpdate(20L)).willReturn(analyzing);

        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FESTIVAL_MAP_INVALID_STATUS)
        );
        assertThat(analyzing.isPublished()).isFalse();
        // 상태를 먼저 걸러야 «부스 없음»이라는 엉뚱한 안내가 나가지 않는다.
        then(nodeService).should(never()).findAll(anyLong(), anyLong());
    }

    @Test
    @DisplayName("현재 지도가 아닌 지도로는 공개할 수 없다")
    void fail_Publish_NotCurrentMap() {
        ReflectionTestUtils.setField(roadmap, "currentMapId", 11L);

        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FESTIVAL_MAP_INVALID_STATUS)
        );
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("축제 정보 수정 권한이 없으면 공개를 거절한다")
    void fail_Publish_Forbidden() {
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createSubAdmin(1L, 20L, 2L));

        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("로그인 정보가 없으면 공개를 거절한다")
    void fail_Publish_Unauthorized() {
        assertThatThrownBy(() -> service.publish(
                festivalPublicId, mapPublicId, null
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
        );
    }

    @Test
    @DisplayName("공개한 부스맵을 내리면 편집 상태로 돌아간다")
    void success_Unpublish() {
        roadmap.publish();

        PublishedRoadmap unpublished = service.unpublish(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(unpublished.roadmapStatus()).isEqualTo(RoadmapStatus.EDITING.name());
        assertThat(unpublished.publishedVersion()).isZero();
        assertThat(unpublished.publishedBoothCount()).isZero();
        assertThat(roadmap.isPublished()).isFalse();
        // 감추기만 할 뿐이라 부스를 세어 볼 필요가 없다.
        then(nodeService).should(never()).findAll(anyLong(), anyLong());
    }

    @Test
    @DisplayName("공개 중이 아닌 부스맵을 내려도 상태가 그대로다")
    void success_Unpublish_WhenNotPublished() {
        PublishedRoadmap unpublished = service.unpublish(
                festivalPublicId, mapPublicId, principal
        );

        assertThat(unpublished.roadmapStatus()).isEqualTo(RoadmapStatus.EDITING.name());
        assertThat(roadmap.isPublished()).isFalse();
    }

    @Test
    @DisplayName("현재 지도가 아닌 지도로는 공개를 해제할 수 없다")
    void fail_Unpublish_NotCurrentMap() {
        roadmap.publish();
        ReflectionTestUtils.setField(roadmap, "currentMapId", 11L);

        assertThatThrownBy(() -> service.unpublish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FESTIVAL_MAP_INVALID_STATUS)
        );
        assertThat(roadmap.isPublished()).isTrue();
    }

    @Test
    @DisplayName("축제 정보 수정 권한이 없으면 공개 해제를 거절한다")
    void fail_Unpublish_Forbidden() {
        roadmap.publish();
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createSubAdmin(1L, 20L, 2L));

        assertThatThrownBy(() -> service.unpublish(
                festivalPublicId, mapPublicId, principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
        assertThat(roadmap.isPublished()).isTrue();
    }

    private RoadmapNode adminBooth(String name) {
        return adminNode(NodeType.BOOTH, name);
    }

    private RoadmapNode adminNode(NodeType type, String name) {
        return RoadmapNode.admin(
                30L, 10L, type, name, GeometryType.POINT,
                "{\"lat\":37.5665,\"lng\":126.9780}", 0, 1L, "2.0"
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
