package com.example.chookjibupadmin.visitor.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FestivalVisitorInputSupportTest {

    @Test
    @DisplayName("DAILY 모드에서 전 일자 완료 시 일자합을 유효 총원으로 쓴다")
    void success_Resolve_DailyReady() {
        Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
        List<FestivalDailyVisitorCount> daily = completeDaily();

        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                daily,
                Optional.empty()
        );

        assertThat(snapshot.status()).isEqualTo(FestivalVisitorInputStatus.READY);
        assertThat(snapshot.source()).isEqualTo(FestivalVisitorEffectiveSource.DAILY_SUM);
        assertThat(snapshot.effectiveVisitorCount()).isEqualTo(600L);
    }

    @Test
    @DisplayName("DAILY 모드에서 총원과 일자합이 다르면 CONFLICT다")
    void success_Resolve_DailyConflict() {
        Festival festival = festival(FestivalVisitorCountInputMode.DAILY);

        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                completeDaily(),
                Optional.of(999)
        );

        assertThat(snapshot.status()).isEqualTo(FestivalVisitorInputStatus.CONFLICT);
        assertThat(snapshot.source()).isEqualTo(FestivalVisitorEffectiveSource.NONE);
        assertThat(snapshot.difference()).isEqualTo(399L);
        assertThat(FestivalVisitorInputSupport.isReportReady(snapshot)).isFalse();
    }

    @Test
    @DisplayName("TOTAL 모드에서 총원만으로 READY가 된다")
    void success_Resolve_TotalReady() {
        Festival festival = festival(FestivalVisitorCountInputMode.TOTAL);

        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                List.of(),
                Optional.of(30000)
        );

        assertThat(snapshot.status()).isEqualTo(FestivalVisitorInputStatus.READY);
        assertThat(snapshot.source()).isEqualTo(FestivalVisitorEffectiveSource.TOTAL);
        assertThat(snapshot.effectiveVisitorCount()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("UNSET에서 일자합과 총원이 다르면 CONFLICT다")
    void success_Resolve_Conflict() {
        Festival festival = festival(FestivalVisitorCountInputMode.UNSET);

        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                completeDaily(),
                Optional.of(999)
        );

        assertThat(snapshot.status()).isEqualTo(FestivalVisitorInputStatus.CONFLICT);
        assertThat(snapshot.source()).isEqualTo(FestivalVisitorEffectiveSource.NONE);
        assertThat(FestivalVisitorInputSupport.isReportReady(snapshot)).isFalse();
    }

    @Test
    @DisplayName("축제 기간 밖 일자 행은 유효 총원 합산에서 제외한다")
    void success_Resolve_IgnoresOutOfPeriodDaily() {
        Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
        List<FestivalDailyVisitorCount> daily = List.of(
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 16),
                        VisitorCount.of(100)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 17),
                        VisitorCount.of(200)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 18),
                        VisitorCount.of(300)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 19),
                        VisitorCount.of(9000)
                )
        );

        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                daily,
                Optional.empty()
        );

        assertThat(snapshot.status()).isEqualTo(FestivalVisitorInputStatus.READY);
        assertThat(snapshot.effectiveVisitorCount()).isEqualTo(600L);
        assertThat(snapshot.dailySum()).isEqualTo(600L);
    }

    private Festival festival(FestivalVisitorCountInputMode mode) {
        Festival festival = Festival.create(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalDetailAddress.of(null),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                ),
                mode
        );
        ReflectionTestUtils.setField(festival, "id", 1L);
        return festival;
    }

    private List<FestivalDailyVisitorCount> completeDaily() {
        return List.of(
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 16),
                        VisitorCount.of(100)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 17),
                        VisitorCount.of(200)
                ),
                FestivalDailyVisitorCount.create(
                        1L,
                        LocalDate.of(2026, 10, 18),
                        VisitorCount.of(300)
                )
        );
    }
}
