package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.FestivalRepository;
import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import com.example.demoadmin.map.command.application.dto.MapAnalysisResultView;
import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import com.example.demoadmin.map.command.domain.MapReviewStatus;
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
class MapAnalysisApplicationServiceIntegrationTest {

    @Autowired
    private MapAnalysisApplicationService mapAnalysisApplicationService;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private FestivalMapService festivalMapService;

    @Autowired
    private MapObjectService mapObjectService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("analyzeTestMap")
    class AnalyzeTestMap {

        @Test
        @DisplayName("테스트 이미지 분석 결과를 DB에 저장한다")
        void success_AnalyzeTestMap_Persisted() {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            CreateTestMapAnalysisCommand command = command();

            // when
            MapAnalysisResultView result = mapAnalysisApplicationService.analyzeTestMap(
                    festival.getPublicId(),
                    command,
                    principal
            );

            // then
            var savedMap = festivalMapService.getByFestivalIdAndPublicId(
                    festival.getId(),
                    result.mapId()
            );
            var savedObjects = mapObjectService.findByFestivalMapId(savedMap.getId());

            assertThat(savedMap.getStatus()).isEqualTo(FestivalMapStatus.ANALYZED);
            assertThat(savedObjects).hasSize(result.objectCount());
            assertThat(savedObjects)
                    .allMatch(object -> object.getReviewStatus() == MapReviewStatus.REVIEW_REQUIRED);
        }
    }

    private CreateTestMapAnalysisCommand command() {
        return new CreateTestMapAnalysisCommand(
                "김밥축제_지적편집도.png",
                "images/김밥축제_지적편집도.png",
                1745,
                1577
        );
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("김밥축제"),
                FestivalDescription.of("김밥축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18)),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0))
        );
    }
}
