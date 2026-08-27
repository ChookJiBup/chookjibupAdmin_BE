package com.example.chookjibupadmin.report.support;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.report.support.dto.FestivalDailyVisitorTrendPoint;
import com.example.chookjibupadmin.report.support.dto.FestivalEconomicEffectMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalOperationEfficiencyMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalTotalVisitorMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalVisitPatternMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalVisitorChangeDirection;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorDaySupport;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorEffectiveSource;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 축제 방문 인원과 시리즈 전년 축제로 성과 리포트 집계 지표를 조립한다.
 */
@Component
@RequiredArgsConstructor
public class FestivalReportMetricAssembler {

    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;

    public FestivalReportMetrics assemble(Festival festival) {
        List<FestivalDailyVisitorCount> currentDaily = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        Optional<Integer> currentTotal = visitorCountService
                .findTotalByFestivalId(festival.getId())
                .map(FestivalTotalVisitorCount::getVisitorCountValue);
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                currentDaily,
                currentTotal
        );

        Optional<Festival> previous = findPreviousFestival(festival);
        long previousTotal = previous
                .map(this::resolveEffectiveTotal)
                .orElse(0L);

        List<FestivalDailyVisitorTrendPoint> dailyTrend =
                buildDailyTrend(festival, currentDaily, previous, snapshot);

        long currentEffective = snapshot.effectiveVisitorCount() == null
                ? 0L
                : snapshot.effectiveVisitorCount();

        return new FestivalReportMetrics(
                festival.getPublicId(),
                festival.getNameValue(),
                festival.getYear() == null ? 0 : festival.getYear(),
                FestivalVisitorDaySupport.totalDayCount(festival),
                FestivalVisitorInputSupport.isReportReady(snapshot),
                buildTotalVisitors(
                        currentEffective,
                        previous.isPresent(),
                        previousTotal
                ),
                dailyTrend,
                FestivalEconomicEffectMetric.unavailable(),
                FestivalOperationEfficiencyMetric.unavailable(),
                List.of(),
                List.of(),
                FestivalVisitPatternMetric.unavailable()
        );
    }

    private List<FestivalDailyVisitorTrendPoint> buildDailyTrend(
            Festival festival,
            List<FestivalDailyVisitorCount> currentDaily,
            Optional<Festival> previous,
            FestivalVisitorInputSupport.FestivalVisitorInputSnapshot snapshot
    ) {
        if (snapshot.inputMode() == FestivalVisitorCountInputMode.TOTAL
                || snapshot.source() == FestivalVisitorEffectiveSource.TOTAL) {
            return List.of();
        }

        Map<LocalDate, Integer> currentByDate = toCountMap(currentDaily);
        Map<Integer, Integer> previousByDayIndex = previous
                .map(this::dailyCountsByDayIndex)
                .orElse(Map.of());

        List<FestivalDailyVisitorTrendPoint> dailyTrend = new ArrayList<>();
        int dayIndex = 1;
        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            Integer currentCount = currentByDate.get(cursor);
            Integer previousCount = previousByDayIndex.get(dayIndex);
            dailyTrend.add(new FestivalDailyVisitorTrendPoint(
                    dayIndex,
                    cursor,
                    currentCount == null ? null : currentCount.longValue(),
                    previousCount == null ? null : previousCount.longValue()
            ));
            dayIndex++;
            cursor = cursor.plusDays(1);
        }
        return dailyTrend;
    }

    private long resolveEffectiveTotal(Festival festival) {
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(
                        festival.getId()
                ),
                visitorCountService.findTotalByFestivalId(festival.getId())
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
        return snapshot.effectiveVisitorCount() == null
                ? 0L
                : snapshot.effectiveVisitorCount();
    }

    private FestivalTotalVisitorMetric buildTotalVisitors(
            long current,
            boolean hasPrevious,
            long previousTotal
    ) {
        if (!hasPrevious) {
            return new FestivalTotalVisitorMetric(
                    current,
                    null,
                    null,
                    null,
                    FestivalVisitorChangeDirection.NONE
            );
        }

        long delta = current - previousTotal;
        BigDecimal rate = null;
        FestivalVisitorChangeDirection direction;
        if (previousTotal == 0L) {
            direction = current == 0L
                    ? FestivalVisitorChangeDirection.FLAT
                    : FestivalVisitorChangeDirection.NONE;
        } else {
            rate = BigDecimal.valueOf(delta)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(previousTotal),
                            1,
                            RoundingMode.HALF_UP
                    );
            if (delta > 0) {
                direction = FestivalVisitorChangeDirection.UP;
            } else if (delta < 0) {
                direction = FestivalVisitorChangeDirection.DOWN;
            } else {
                direction = FestivalVisitorChangeDirection.FLAT;
            }
        }

        return new FestivalTotalVisitorMetric(
                current,
                previousTotal,
                delta,
                rate,
                direction
        );
    }

    private Optional<Festival> findPreviousFestival(Festival festival) {
        if (festival.getSeriesId() == null || festival.getYear() == null) {
            return Optional.empty();
        }
        return festivalService.findBySeriesIdAndYear(
                festival.getSeriesId(),
                festival.getYear() - 1
        );
    }

    private Map<Integer, Integer> dailyCountsByDayIndex(Festival festival) {
        List<FestivalDailyVisitorCount> daily = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        Map<LocalDate, Integer> byDate = toCountMap(daily);
        Map<Integer, Integer> byIndex = new java.util.HashMap<>();
        int dayIndex = 1;
        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            Integer count = byDate.get(cursor);
            if (count != null) {
                byIndex.put(dayIndex, count);
            }
            dayIndex++;
            cursor = cursor.plusDays(1);
        }
        return byIndex;
    }

    private Map<LocalDate, Integer> toCountMap(
            List<FestivalDailyVisitorCount> dailyCounts
    ) {
        return dailyCounts.stream().collect(Collectors.toMap(
                FestivalDailyVisitorCount::getVisitDate,
                FestivalDailyVisitorCount::getVisitorCountValue,
                (left, right) -> right
        ));
    }
}
