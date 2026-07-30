package com.example.demoadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.command.application.dto.BoothQueueTailResult;
import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.CreateBoothQueueLineCommand;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueTailCommand;
import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.FestivalRepository;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
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
class BoothApplicationServiceIntegrationTest {

    @Autowired
    private BoothApplicationService boothApplicationService;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("createQueueLine")
    class CreateQueueLine {

        @Test
        @DisplayName("축제 부스와 대기 라인을 저장하고 줄 끝을 갱신한다")
        void success_CreateQueueLine() {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = principal();
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            var booth = boothApplicationService.createBooth(
                    festival.getPublicId(),
                    boothCommand(),
                    principal
            );
            var queueLine = boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    queueLineCommand(1),
                    principal
            );

            // when
            BoothQueueTailResult result = boothApplicationService.updateQueueTail(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    new UpdateBoothQueueTailCommand(
                            queueLine.getPublicId(),
                            "OPERATING"
                    ),
                    principal
            );

            // then
            assertThat(result.booth().getOperatingStatus())
                    .isEqualTo(BoothOperatingStatus.OPERATING);
            assertThat(result.currentQueueLine().getPublicId())
                    .isEqualTo(queueLine.getPublicId());
        }

        @Test
        @DisplayName("같은 부스에 같은 순서의 대기 라인을 저장할 수 없다")
        void fail_CreateQueueLine_CustomException_DuplicatedLineOrder() {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = principal();
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            var booth = boothApplicationService.createBooth(
                    festival.getPublicId(),
                    boothCommand(),
                    principal
            );
            boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    queueLineCommand(1),
                    principal
            );

            // when & then
            assertThatThrownBy(() -> boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    booth.getPublicId(),
                    queueLineCommand(1),
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.BOOTH_QUEUE_LINE_ORDER_DUPLICATED.getMessage()
                    );
        }
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "owner@mapo.go.kr");
    }

    private CreateBoothCommand boothCommand() {
        return new CreateBoothCommand(
                "푸드 부스",
                "먹거리",
                "A-1",
                "대표 먹거리 부스"
        );
    }

    private CreateBoothQueueLineCommand queueLineCommand(int lineOrder) {
        return new CreateBoothQueueLineCommand(
                lineOrder,
                "라인 " + lineOrder,
                10,
                30,
                "{}",
                "{}"
        );
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
