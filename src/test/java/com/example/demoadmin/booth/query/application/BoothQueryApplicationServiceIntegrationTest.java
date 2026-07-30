package com.example.demoadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.command.application.BoothApplicationService;
import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.CreateBoothQueueLineCommand;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.FestivalRepository;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BoothQueryApplicationServiceIntegrationTest {

    @Autowired
    private BoothQueryApplicationService boothQueryApplicationService;

    @Autowired
    private BoothApplicationService boothApplicationService;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("getQueueLines")
    class GetQueueLines {

        @Test
        @DisplayName("축제와 부스 UUID로 대기 라인 projection을 조회한다")
        void success_GetQueueLines() {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            var booth = boothApplicationService.createBooth(
                    festival.getPublicId(),
                    new CreateBoothCommand("푸드 부스", "먹거리", "A-1", "설명"),
                    principal
            );
            boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new CreateBoothQueueLineCommand(
                            1,
                            "라인 1",
                            10,
                            30,
                            "{}",
                            "{}"
                    ),
                    principal
            );

            // when
            List<BoothQueueLineView> result =
                    boothQueryApplicationService.getQueueLines(
                            festival.getPublicId(),
                            booth.getPublicId(),
                            principal
                    );

            // then
            assertThat(result)
                    .singleElement()
                    .extracting(BoothQueueLineView::lineOrder)
                    .isEqualTo(1);
        }
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("김밥축제"),
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
