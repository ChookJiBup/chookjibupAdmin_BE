package com.example.demoadmin.map.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.map.command.application.FestivalMapService;
import com.example.demoadmin.map.command.application.MapObjectService;
import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import com.example.demoadmin.map.query.application.dto.FestivalMapObjectsView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapQueryApplicationServiceIntegrationTest {

    @Autowired
    private MapQueryApplicationService applicationService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private FestivalMapService festivalMapService;

    @Autowired
    private MapObjectService mapObjectService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("getMapObjects")
    class GetMapObjects {

        @Test
        @DisplayName("담당 축제의 배치도와 객체 Projection을 조회한다")
        void success_GetMapObjects_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            FestivalMap festivalMap = festivalMapService.save(
                    festivalMap(festival.getId())
            );
            mapObjectService.save(mapObject(festivalMap.getId()));

            // when
            FestivalMapObjectsView result = applicationService.getMapObjects(
                    festival.getPublicId(),
                    festivalMap.getPublicId(),
                    principal
            );

            // then
            assertThat(result.map().mapId()).isEqualTo(festivalMap.getPublicId());
            assertThat(result.objects()).hasSize(1);
            assertThat(result.objects().getFirst().name()).isEqualTo("김밥 부스");
        }
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("김밥축제"),
                FestivalDescription.of("김밥축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }

    private FestivalMap festivalMap(Long festivalId) {
        FestivalMap festivalMap = FestivalMap.create(
                festivalId,
                MapFileName.of("김밥축제_지적편집도.png"),
                MapStorageType.TEST_RESOURCE,
                MapStoragePath.of("images/김밥축제_지적편집도.png"),
                1745,
                1577
        );
        festivalMap.markAnalyzed();
        return festivalMap;
    }

    private MapObject mapObject(Long festivalMapId) {
        return MapObject.createAiGenerated(
                festivalMapId,
                1L,
                MapObjectType.FOOD_BOOTH,
                MapObjectName.of("김밥 부스"),
                GeometryType.RECTANGLE,
                GeometryData.of(
                        "{\"type\":\"RECTANGLE\",\"x\":0.31,\"y\":0.22,"
                                + "\"width\":0.08,\"height\":0.05}"
                ),
                ConfidenceScore.of(0.82)
        );
    }
}
