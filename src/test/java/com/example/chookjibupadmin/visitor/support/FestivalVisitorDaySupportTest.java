package com.example.chookjibupadmin.visitor.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FestivalVisitorDaySupportTest {

    @Test
    @DisplayName("축제 기간 일수를 계산한다")
    void success_TotalDayCount() {
        Festival festival = festival();

        assertThat(FestivalVisitorDaySupport.totalDayCount(festival)).isEqualTo(3);
    }

    @Test
    @DisplayName("모든 일자가 채워졌는지 판별한다")
    void success_IsAllDaysFilled() {
        Festival festival = festival();
        List<FestivalDailyVisitorCount> incomplete = List.of(
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 16),
                        VisitorCount.of(10)
                )
        );
        List<FestivalDailyVisitorCount> complete = List.of(
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 16),
                        VisitorCount.of(10)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 17),
                        VisitorCount.of(20)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 18),
                        VisitorCount.of(30)
                )
        );

        assertThat(FestivalVisitorDaySupport.isAllDaysFilled(festival, incomplete))
                .isFalse();
        assertThat(FestivalVisitorDaySupport.isAllDaysFilled(festival, complete))
                .isTrue();
    }

    @Test
    @DisplayName("총원만 있어도 리포트 입력이 완료된다")
    void success_IsVisitorInputReady_TotalOnly() {
        Festival festival = festival();

        assertThat(FestivalVisitorDaySupport.isVisitorInputReady(
                festival,
                List.of(),
                true
        )).isTrue();
        assertThat(FestivalVisitorDaySupport.isVisitorInputReady(
                festival,
                List.of(),
                false
        )).isFalse();
    }

    private Festival festival() {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
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
        ReflectionTestUtils.setField(festival, "id", 1L);
        return festival;
    }
}
