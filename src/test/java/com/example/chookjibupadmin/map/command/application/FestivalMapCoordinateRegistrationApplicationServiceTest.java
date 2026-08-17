package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapStatus;
import java.math.BigDecimal;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalMapCoordinateRegistrationApplicationServiceTest {

    @InjectMocks
    private FestivalMapCoordinateRegistrationApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService roleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalLocationService festivalLocationService;
    @Mock private FestivalMapService mapService;
    @Mock private FestivalRoadmapService roadmapService;

    private final UUID festivalPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
    private Festival festival;
    private FestivalLocation primaryLocation;

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
        festival = Festival.create(
                1L,
                festivalPublicId,
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(20, 0))
        );
        ReflectionTestUtils.setField(festival, "id", 20L);
        primaryLocation = FestivalLocation.create(
                festival,
                FestivalLocationType.MAIN_VENUE,
                "본행사장",
                "서울특별시 마포구",
                null,
                null,
                null,
                null,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                true,
                0,
                1L
        );
        ReflectionTestUtils.setField(primaryLocation, "id", 5L);

        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festivalPublicId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
    }

    @Test
    @DisplayName("primary 장소 위경도가 없으면 좌표 지도를 만들 수 없다")
    void fail_EnsureCoordinateMap_LocationRequired() {
        FestivalLocation missing = FestivalLocation.create(
                festival,
                FestivalLocationType.MAIN_VENUE,
                "본행사장",
                "서울특별시 마포구",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                0,
                1L
        );
        given(mapService.findCurrentByFestivalId(20L)).willReturn(Optional.empty());
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of(missing));

        assertThatThrownBy(() -> service.ensureCoordinateMap(
                festivalPublicId, "본행사 배치", principal
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED)
        );

        then(mapService).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("current map이 있으면 새로 만들지 않고 기존 mapId를 반환한다")
    void success_EnsureCoordinateMap_ReuseCurrent() {
        FestivalMap current = FestivalMap.coordinateOnly(
                20L, 5L, FestivalMapName.of("본행사 배치"), 1L
        );
        ReflectionTestUtils.setField(current, "id", 10L);
        FestivalRoadmap roadmap = FestivalRoadmap.createForCoordinateMap(20L, 10L, 1L);
        given(mapService.findCurrentByFestivalId(20L)).willReturn(Optional.of(current));
        given(roadmapService.getByFestivalId(20L)).willReturn(roadmap);
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of(primaryLocation));

        var view = service.ensureCoordinateMap(festivalPublicId, "본행사 배치", principal);

        assertThat(view.mapId()).isEqualTo(current.getPublicId());
        assertThat(view.roadmapStatus()).isEqualTo(RoadmapStatus.EDITING.name());
        then(mapService).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("좌표 지도가 없으면 map과 EDITING roadmap을 생성한다")
    void success_EnsureCoordinateMap_Create() {
        given(mapService.findCurrentByFestivalId(20L)).willReturn(Optional.empty());
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of(primaryLocation));
        given(mapService.save(org.mockito.ArgumentMatchers.any(FestivalMap.class)))
                .willAnswer(invocation -> {
                    FestivalMap map = invocation.getArgument(0);
                    ReflectionTestUtils.setField(map, "id", 10L);
                    return map;
                });
        given(roadmapService.findByFestivalId(20L)).willReturn(Optional.empty());
        given(roadmapService.save(org.mockito.ArgumentMatchers.any(FestivalRoadmap.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(roadmapService.getByFestivalId(20L))
                .willAnswer(invocation -> FestivalRoadmap.createForCoordinateMap(20L, 10L, 1L));

        var view = service.ensureCoordinateMap(festivalPublicId, "본행사 배치", principal);

        assertThat(view.mapName()).isEqualTo("본행사 배치");
        assertThat(view.center().lat()).isEqualByComparingTo("37.5665");
        assertThat(view.roadmapStatus()).isEqualTo(RoadmapStatus.EDITING.name());
        then(mapService).should().save(org.mockito.ArgumentMatchers.any(FestivalMap.class));
        then(roadmapService).should().save(org.mockito.ArgumentMatchers.any(FestivalRoadmap.class));
    }
}
