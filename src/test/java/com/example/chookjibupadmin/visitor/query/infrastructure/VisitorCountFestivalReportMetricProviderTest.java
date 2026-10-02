package com.example.chookjibupadmin.visitor.query.infrastructure;

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
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VisitorCountFestivalReportMetricProviderTest {

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalVisitorCountService visitorCountService;

    @Mock
    private Clock clock;

    @InjectMocks
    private VisitorCountFestivalReportMetricProvider provider;

    @Nested
    @DisplayName("findSummary")
    class FindSummary {

        @Test
        @DisplayName("DAILY 모드에서는 일자 합계를 유효 총원으로 반환한다")
        void success_FindSummary_DailyModeUsesDailySum() {
            Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(
                            daily(festival.getId(), LocalDate.of(2026, 10, 16), 400),
                            daily(festival.getId(), LocalDate.of(2026, 10, 17), 300),
                            daily(festival.getId(), LocalDate.of(2026, 10, 18), 300)
                    ));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.of(total(10L, 1000)));

            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().totalVisitorCount()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("DAILY 모드에서 총원 보조값이 달라도 총원을 우선하지 않고 비운다")
        void success_FindSummary_DailyModeDoesNotPreferConflictingTotal() {
            Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(
                            daily(festival.getId(), LocalDate.of(2026, 10, 16), 400),
                            daily(festival.getId(), LocalDate.of(2026, 10, 17), 300),
                            daily(festival.getId(), LocalDate.of(2026, 10, 18), 300)
                    ));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.of(total(10L, 1200)));

            assertThat(provider.findSummary(10L)).isEmpty();
        }

        @Test
        @DisplayName("TOTAL 모드에서는 총원을 반환한다")
        void success_FindSummary_TotalMode() {
            Festival festival = festival(FestivalVisitorCountInputMode.TOTAL);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of());
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.of(total(10L, 30000)));

            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().totalVisitorCount()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("DAILY 충돌이면 요약을 비운다")
        void success_FindSummary_DailyConflictEmpty() {
            Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(
                            daily(festival.getId(), LocalDate.of(2026, 10, 16), 400),
                            daily(festival.getId(), LocalDate.of(2026, 10, 17), 300),
                            daily(festival.getId(), LocalDate.of(2026, 10, 18), 300)
                    ));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.of(total(10L, 999)));

            assertThat(provider.findSummary(10L)).isEmpty();
        }

        @Test
        @DisplayName("방문 인원 데이터가 없으면 빈 값을 반환한다")
        void success_FindSummary_Empty() {
            Festival festival = festival(FestivalVisitorCountInputMode.UNSET);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of());
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.empty());

            assertThat(provider.findSummary(10L)).isEmpty();
        }

        @Test
        @DisplayName("진행 중 DAILY 모드는 오늘까지 입력된 부분합을 반환한다")
        void success_FindSummary_OngoingDailyPartialSumThroughToday() {
            Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
            given(clock.instant()).willReturn(Instant.parse("2026-10-17T00:00:00Z"));
            given(clock.getZone()).willReturn(ZoneOffset.UTC);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(
                            daily(festival.getId(), LocalDate.of(2026, 10, 16), 400),
                            // 미래 일차 값이 잘못 들어와도 실시간 누적에는 섞지 않는다.
                            daily(festival.getId(), LocalDate.of(2026, 10, 18), 900)
                    ));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.empty());

            Optional<FestivalReportMetricProvider.Snapshot> snapshot =
                    provider.findSummary(10L);

            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().totalVisitorCount()).isEqualTo(400L);
        }

        @Test
        @DisplayName("수동 종료 상태에서는 날짜상 진행 중이어도 부분합을 운영 지표로 제공하지 않는다")
        void success_FindSummary_ManualCompletedDoesNotExposeOngoingPartialSum() {
            Festival festival = festival(FestivalVisitorCountInputMode.DAILY);
            festival.changeProgressStatus(FestivalProgressStatus.COMPLETED);
            given(clock.instant()).willReturn(Instant.parse("2026-10-17T00:00:00Z"));
            given(clock.getZone()).willReturn(ZoneOffset.UTC);
            given(festivalService.getById(10L)).willReturn(festival);
            given(visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(10L))
                    .willReturn(List.of(
                            daily(festival.getId(), LocalDate.of(2026, 10, 16), 400)
                    ));
            given(visitorCountService.findTotalByFestivalId(10L))
                    .willReturn(Optional.empty());

            assertThat(provider.findSummary(10L)).isEmpty();
        }
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
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0)),
                mode
        );
        ReflectionTestUtils.setField(festival, "id", 10L);
        return festival;
    }

    private FestivalDailyVisitorCount daily(Long festivalId, LocalDate date, int count) {
        return FestivalDailyVisitorCount.create(festivalId, date, VisitorCount.of(count));
    }

    private FestivalTotalVisitorCount total(Long festivalId, int count) {
        return FestivalTotalVisitorCount.create(festivalId, VisitorCount.of(count));
    }
}
