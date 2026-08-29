package com.example.chookjibupadmin.dashboard.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapZone;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoothCongestionFestivalDashboardMetricProviderTest {

    private BoothCongestionFestivalDashboardMetricProvider provider;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private BoothCongestionService boothCongestionService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalRoadmapService festivalRoadmapService;

    @Mock
    private RoadmapNodeService roadmapNodeService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        provider = new BoothCongestionFestivalDashboardMetricProvider(
                boothInfoService,
                boothCongestionService,
                festivalService,
                festivalRoadmapService,
                roadmapNodeService,
                adminAccountService,
                fieldStaffAccountService,
                new ObjectMapper(),
                clock
        );
    }

    @Nested
    @DisplayName("findCurrent")
    class FindCurrent {

        @Test
        @DisplayName("부스·혼잡이 없고 구역만 있으면 스냅샷을 만들지 않는다")
        void success_FindCurrent_ZonesOnly_Empty() {
            Festival festival = festival();
            given(festivalService.getById(10L)).willReturn(festival);
            stubClock();
            given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of());
            given(boothCongestionService.findLatestByBoothIds(List.of()))
                    .willReturn(List.of());
            given(festivalRoadmapService.findByFestivalId(10L))
                    .willReturn(Optional.of(roadmapWithZone(UUID.randomUUID())));

            Optional<FestivalDashboardMetricProvider.Snapshot> result =
                    provider.findCurrent(10L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("부스만 있으면 visitorAvailable은 false이고 스냅샷은 반환한다")
        void success_FindCurrent_BoothsWithoutCongestion() {
            Festival festival = festival();
            BoothInfo booth = BoothInfo.create(10L, 20L, "테스트부스");
            ReflectionTestUtils.setField(booth, "id", 1L);
            stubClock();
            given(festivalService.getById(10L)).willReturn(festival);
            given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
            given(boothCongestionService.findLatestByBoothIds(List.of(1L)))
                    .willReturn(List.of());
            given(roadmapNodeService.findAllById(anySet())).willReturn(List.of());
            given(festivalRoadmapService.findByFestivalId(10L))
                    .willReturn(Optional.empty());

            FestivalDashboardMetricProvider.Snapshot snapshot =
                    provider.findCurrent(10L).orElseThrow();

            assertThat(snapshot.visitorAvailable()).isFalse();
            assertThat(snapshot.boothAvailable()).isTrue();
            assertThat(snapshot.congestionAvailable()).isFalse();
            assertThat(snapshot.currentVisitorCount()).isNull();
            assertThat(snapshot.zones()).isEmpty();
            verify(adminAccountService, never()).findAllById(anyCollection());
        }

        @Test
        @DisplayName("부스 혼잡이 있으면 대기 지표는 채우고 현재 방문자는 비운다")
        void success_FindCurrent_BoothsWithCongestion() {
            Festival festival = festival();
            BoothInfo booth = BoothInfo.create(10L, 20L, "테스트부스");
            ReflectionTestUtils.setField(booth, "id", 1L);
            BoothCongestion congestion = BoothCongestion.recordByAdmin(
                    1L,
                    1L,
                    12,
                    BoothCongestionLevel.HIGH
            );
            stubClock();
            given(festivalService.getById(10L)).willReturn(festival);
            given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
            given(boothCongestionService.findLatestByBoothIds(List.of(1L)))
                    .willReturn(List.of(congestion));
            given(roadmapNodeService.findAllById(anySet())).willReturn(List.of());
            given(adminAccountService.findAllById(anyCollection())).willReturn(List.of());
            given(festivalRoadmapService.findByFestivalId(10L))
                    .willReturn(Optional.empty());

            FestivalDashboardMetricProvider.Snapshot snapshot =
                    provider.findCurrent(10L).orElseThrow();

            assertThat(snapshot.visitorAvailable()).isFalse();
            assertThat(snapshot.boothAvailable()).isTrue();
            assertThat(snapshot.congestionAvailable()).isTrue();
            assertThat(snapshot.averageWaitMinutes()).isEqualTo(12L);
            assertThat(snapshot.currentVisitorCount()).isNull();
        }

        @Test
        @DisplayName("WGS84 POINT 노드·수정자·zones를 부스 지표에 채운다")
        void success_FindCurrent_EnrichmentFields() {
            Festival festival = festival();
            UUID nodePublicId = UUID.randomUUID();
            UUID zoneId = UUID.randomUUID();
            BoothInfo booth = BoothInfo.create(10L, 20L, "좌표부스");
            ReflectionTestUtils.setField(booth, "id", 1L);
            RoadmapNode node = RoadmapNode.admin(
                    nodePublicId,
                    1L,
                    1L,
                    NodeType.BOOTH,
                    "좌표부스",
                    GeometryType.POINT,
                    "{\"lat\":37.5665,\"lng\":126.9780}",
                    0,
                    99L,
                    "2.0"
            );
            ReflectionTestUtils.setField(node, "id", 20L);
            BoothCongestion congestion = BoothCongestion.recordByAdmin(
                    1L,
                    99L,
                    15,
                    BoothCongestionLevel.MEDIUM
            );
            AdminAccount admin = AdminAccount.createAdmin(
                    AdminEmail.of("owner@example.com"),
                    AdminName.of("홍길동"),
                    AdminOrganization.of("축집"),
                    AdminRank.of("담당"),
                    AdminPasswordHash.of("hash")
            );
            ReflectionTestUtils.setField(admin, "id", 99L);

            stubClock();
            given(festivalService.getById(10L)).willReturn(festival);
            given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
            given(boothCongestionService.findLatestByBoothIds(List.of(1L)))
                    .willReturn(List.of(congestion));
            given(roadmapNodeService.findAllById(anySet())).willReturn(List.of(node));
            given(adminAccountService.findAllById(anyCollection()))
                    .willReturn(List.of(admin));
            given(festivalRoadmapService.findByFestivalId(10L))
                    .willReturn(Optional.of(roadmapWithZone(zoneId)));

            FestivalDashboardMetricProvider.Snapshot snapshot =
                    provider.findCurrent(10L).orElseThrow();

            FestivalDashboardMetricProvider.BoothMetric boothMetric =
                    snapshot.booths().getFirst();
            assertThat(boothMetric.roadmapNodePublicId()).isEqualTo(nodePublicId);
            assertThat(boothMetric.lat()).isEqualByComparingTo(new BigDecimal("37.5665"));
            assertThat(boothMetric.lng()).isEqualByComparingTo(new BigDecimal("126.9780"));
            assertThat(boothMetric.modifierType()).isEqualTo("ADMIN");
            assertThat(boothMetric.modifierAdminId()).isEqualTo(99L);
            assertThat(boothMetric.modifierName()).isEqualTo("홍길동");
            assertThat(snapshot.zones()).hasSize(1);
            assertThat(snapshot.zones().getFirst().zoneId()).isEqualTo(zoneId);
            assertThat(snapshot.zones().getFirst().name()).isEqualTo("A구역");
            verify(adminAccountService).findAllById(anyCollection());
            verify(adminAccountService, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("schema 1.0 POINT는 lat/lng를 채우지 않는다")
        void success_FindCurrent_LegacyGeometry_NoLatLng() {
            Festival festival = festival();
            BoothInfo booth = BoothInfo.create(10L, 20L, "레거시부스");
            ReflectionTestUtils.setField(booth, "id", 1L);
            RoadmapNode node = RoadmapNode.admin(
                    1L,
                    1L,
                    NodeType.BOOTH,
                    "레거시",
                    GeometryType.POINT,
                    "{\"x\":100,\"y\":200}",
                    0,
                    1L,
                    "1.0"
            );
            ReflectionTestUtils.setField(node, "id", 20L);
            stubClock();
            given(festivalService.getById(10L)).willReturn(festival);
            given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
            given(boothCongestionService.findLatestByBoothIds(List.of(1L)))
                    .willReturn(List.of());
            given(roadmapNodeService.findAllById(anySet())).willReturn(List.of(node));
            given(festivalRoadmapService.findByFestivalId(10L))
                    .willReturn(Optional.empty());

            FestivalDashboardMetricProvider.BoothMetric boothMetric =
                    provider.findCurrent(10L).orElseThrow().booths().getFirst();

            assertThat(boothMetric.roadmapNodePublicId()).isEqualTo(node.getPublicId());
            assertThat(boothMetric.lat()).isNull();
            assertThat(boothMetric.lng()).isNull();
        }
    }

    private void stubClock() {
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
        given(clock.instant()).willReturn(
                LocalDate.of(2026, 8, 21).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }

    private Festival festival() {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("대시보드 검증 축제"),
                FestivalDescription.of("검증용"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 22)
                ),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0))
        );
        ReflectionTestUtils.setField(festival, "id", 10L);
        return festival;
    }

    private FestivalRoadmap roadmapWithZone(UUID zoneId) {
        FestivalRoadmap roadmap = FestivalRoadmap.create(10L, 1L, 1L);
        ReflectionTestUtils.setField(
                roadmap,
                "zones",
                List.of(new RoadmapZone(zoneId, "A구역", 0, List.of()))
        );
        return roadmap;
    }
}
