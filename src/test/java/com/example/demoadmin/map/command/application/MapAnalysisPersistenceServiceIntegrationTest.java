package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.FestivalRepository;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import com.example.demoadmin.map.command.domain.MapAnalysisStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MapAnalysisPersistenceServiceIntegrationTest {

    @Autowired
    private MapAnalysisPersistenceService mapAnalysisPersistenceService;

    @Autowired
    private MapAnalysisJobService mapAnalysisJobService;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("분석 실패 상태와 사유를 별도 트랜잭션으로 저장한다")
        void success_Fail() {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = new AdminPrincipal(901L, "owner901@mapo.go.kr");
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            var prepared = mapAnalysisPersistenceService.prepare(
                    festival.getPublicId(),
                    command(),
                    principal
            );

            // when
            mapAnalysisPersistenceService.fail(
                    prepared.analysisJobId(),
                    "외부 분석 실패"
            );

            // then
            var analysisJob = mapAnalysisJobService.getById(
                    prepared.analysisJobId()
            );
            assertThat(analysisJob.getStatus()).isEqualTo(MapAnalysisStatus.FAILED);
            assertThat(analysisJob.getFailureReason()).isEqualTo("외부 분석 실패");
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
                901L,
                UUID.randomUUID(),
                FestivalName.of("실패 상태 검증 축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
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
}
