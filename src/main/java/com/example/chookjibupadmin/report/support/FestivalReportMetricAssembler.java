package com.example.chookjibupadmin.report.support;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
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
        Map<LocalDate, Integer> currentByDate = toCountMap(currentDaily);
        Optional<FestivalTotalVisitorCount> currentTotalEntity =
                visitorCountService.findTotalByFestivalId(festival.getId());
        boolean completed = FestivalVisitorDaySupport.isVisitorInputReady(
                festival,
                currentDaily,
                currentTotalEntity.isPresent()
        );

        Optional<Festival> previous = findPreviousFestival(festival);
        Map<Integer, Integer> previousByDayIndex = previous
                .map(this::dailyCountsByDayIndex)
                .orElse(Map.of());
        long previousTotal = previous
                .map(this::resolveTotalVisitorCount)
                .orElse(0L);

        List<FestivalDailyVisitorTrendPoint> dailyTrend = new ArrayList<>();
        long dailySum = 0L;
        int dayIndex = 1;
        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            Integer currentCount = currentByDate.get(cursor);
            if (currentCount != null) {
                dailySum += currentCount;
            }
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

        long currentTotal = currentTotalEntity
                .map(value -> (long) value.getVisitorCountValue())
                .orElse(dailySum);

        return new FestivalReportMetrics(
                festival.getPublicId(),
                festival.getNameValue(),
                festival.getYear() == null ? 0 : festival.getYear(),
                FestivalVisitorDaySupport.totalDayCount(festival),
                completed,
                buildTotalVisitors(currentTotal, previous.isPresent(), previousTotal),
                dailyTrend,
                FestivalEconomicEffectMetric.unavailable(),
                FestivalOperationEfficiencyMetric.unavailable(),
                List.of(),
                List.of(),
                FestivalVisitPatternMetric.unavailable()
        );
    }

    private long resolveTotalVisitorCount(Festival festival) {
        Optional<FestivalTotalVisitorCount> total = visitorCountService
                .findTotalByFestivalId(festival.getId());
        if (total.isPresent()) {
            return total.get().getVisitorCountValue();
        }
        return visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId())
                .stream()
                .mapToLong(FestivalDailyVisitorCount::getVisitorCountValue)
                .sum();
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
