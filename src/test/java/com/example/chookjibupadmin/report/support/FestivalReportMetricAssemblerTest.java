package com.example.chookjibupadmin.report.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalReportMetricAssemblerTest {

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalVisitorCountService visitorCountService;

    @Test
    @DisplayName("진행 중 DAILY 리포트는 오늘까지의 누적과 추이만 반영한다")
    void success_Assemble_OngoingDailyThroughToday() {
        Festival festival = festival(
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 19)
        );
        given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                .willReturn(List.of(
                        daily(LocalDate.of(2026, 10, 16), 100),
                        daily(LocalDate.of(2026, 10, 17), 200),
                        daily(LocalDate.of(2026, 10, 18), 900)
                ));
        given(visitorCountService.findTotalByFestivalId(10L))
                .willReturn(Optional.empty());

        FestivalReportMetrics metrics = assembler("2026-10-17T12:00:00Z")
                .assemble(festival);

        assertThat(metrics.totalVisitors().current()).isEqualTo(300L);
        assertThat(metrics.dailyTrend())
                .extracting(point -> point.visitDate())
                .containsExactly(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 17)
                );
        assertThat(metrics.dailyTrend())
                .extracting(point -> point.currentCount())
                .containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("종료된 DAILY 리포트는 종료일까지 전체 기간을 반영한다")
    void success_Assemble_CompletedDailyThroughEndDate() {
        Festival festival = festival(
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18)
        );
        given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                .willReturn(List.of(
                        daily(LocalDate.of(2026, 10, 16), 100),
                        daily(LocalDate.of(2026, 10, 17), 200),
                        daily(LocalDate.of(2026, 10, 18), 300)
                ));
        given(visitorCountService.findTotalByFestivalId(10L))
                .willReturn(Optional.empty());

        FestivalReportMetrics metrics = assembler("2026-10-19T00:00:00Z")
                .assemble(festival);

        assertThat(metrics.totalVisitors().current()).isEqualTo(600L);
        assertThat(metrics.dailyTrend()).hasSize(3);
        assertThat(metrics.dailyTrend().get(2).visitDate())
                .isEqualTo(LocalDate.of(2026, 10, 18));
    }

    private FestivalReportMetricAssembler assembler(String instant) {
        return new FestivalReportMetricAssembler(
                festivalService,
                visitorCountService,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
        );
    }

    private Festival festival(LocalDate startDate, LocalDate endDate) {
        Festival festival = Festival.create(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalDetailAddress.of(null),
                FestivalPeriod.of(startDate, endDate),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0)),
                FestivalVisitorCountInputMode.DAILY
        );
        ReflectionTestUtils.setField(festival, "id", 10L);
        return festival;
    }

    private FestivalDailyVisitorCount daily(LocalDate date, int count) {
        return FestivalDailyVisitorCount.create(10L, date, VisitorCount.of(count));
    }
}
