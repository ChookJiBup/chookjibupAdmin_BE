package com.example.demoadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.CreateBoothQueueLineCommand;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BoothConcurrencyIntegrationTest {

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
        @DisplayName("동시에 같은 순서를 생성해도 하나만 저장한다")
        void success_CreateQueueLine_ConcurrentDuplicate() throws Exception {
            // given
            Festival festival = festivalRepository.save(festival());
            AdminPrincipal principal = new AdminPrincipal(9901L, "owner9901@mapo.go.kr");
            adminFestivalRoleService.assignFestivalOwner(
                    principal.adminId(),
                    festival.getId()
            );
            var booth = boothApplicationService.createBooth(
                    festival.getPublicId(),
                    new CreateBoothCommand("동시성 부스", "먹거리", "A-1", "설명"),
                    principal
            );
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                Future<String> first = executor.submit(() -> createQueueLine(
                        start,
                        festival,
                        booth.getPublicId(),
                        principal
                ));
                Future<String> second = executor.submit(() -> createQueueLine(
                        start,
                        festival,
                        booth.getPublicId(),
                        principal
                ));

                // when
                start.countDown();
                String firstResult = first.get(10, TimeUnit.SECONDS);
                String secondResult = second.get(10, TimeUnit.SECONDS);

                // then
                assertThat(firstResult + "," + secondResult)
                        .contains("SUCCESS")
                        .contains(ErrorCode.BOOTH_QUEUE_LINE_ORDER_DUPLICATED.getMessage());
                assertThat(firstResult).isNotEqualTo(secondResult);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private String createQueueLine(
            CountDownLatch start,
            Festival festival,
            UUID boothId,
            AdminPrincipal principal
    ) throws InterruptedException {
        start.await();
        try {
            boothApplicationService.createQueueLine(
                    festival.getPublicId(),
                    boothId,
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
            return "SUCCESS";
        } catch (CustomException exception) {
            return exception.getMessage();
        }
    }

    private Festival festival() {
        return Festival.create(
                9901L,
                UUID.randomUUID(),
                FestivalName.of("동시성 검증 축제"),
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
