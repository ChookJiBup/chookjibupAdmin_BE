package com.example.chookjibupadmin.visitor.support;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import java.util.List;
import java.util.Optional;

/**
 * 방문 인원 투트랙의 유효 총원·상태를 해석한다.
 */
public final class FestivalVisitorInputSupport {

    private FestivalVisitorInputSupport() {
    }

    public static FestivalVisitorInputSnapshot resolve(
            Festival festival,
            List<FestivalDailyVisitorCount> dailyCounts,
            Optional<Integer> totalVisitorCount
    ) {
        FestivalVisitorCountInputMode mode = festival.getVisitorCountInputMode()
                == null
                ? FestivalVisitorCountInputMode.UNSET
                : festival.getVisitorCountInputMode();
        List<FestivalDailyVisitorCount> periodCounts =
                FestivalVisitorDaySupport.withinFestivalPeriod(festival, dailyCounts);
        boolean allDaysFilled = FestivalVisitorDaySupport.isAllDaysFilled(
                festival,
                periodCounts
        );
        long dailySum = periodCounts.stream()
                .mapToLong(FestivalDailyVisitorCount::getVisitorCountValue)
                .sum();
        boolean totalEntered = totalVisitorCount.isPresent();
        Integer totalValue = totalVisitorCount.orElse(null);
        Long difference = null;
        if (allDaysFilled && totalEntered) {
            difference = totalValue.longValue() - dailySum;
        }

        return switch (mode) {
            case DAILY -> resolveDaily(
                    allDaysFilled,
                    dailySum,
                    totalEntered,
                    totalValue,
                    difference
            );
            case TOTAL -> resolveTotal(totalEntered, totalValue, difference);
            case UNSET -> resolveUnset(
                    allDaysFilled,
                    dailySum,
                    totalEntered,
                    totalValue,
                    difference,
                    periodCounts.isEmpty()
            );
        };
    }

    public static boolean isReportReady(FestivalVisitorInputSnapshot snapshot) {
        return snapshot.status() == FestivalVisitorInputStatus.READY;
    }

    private static FestivalVisitorInputSnapshot resolveDaily(
            boolean allDaysFilled,
            long dailySum,
            boolean totalEntered,
            Integer totalValue,
            Long difference
    ) {
        if (!allDaysFilled) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.DAILY,
                    FestivalVisitorInputStatus.PARTIAL,
                    FestivalVisitorEffectiveSource.NONE,
                    null,
                    dailySum,
                    totalEntered,
                    totalValue,
                    difference
            );
        }
        if (totalEntered && difference != null && difference != 0L) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.DAILY,
                    FestivalVisitorInputStatus.CONFLICT,
                    FestivalVisitorEffectiveSource.NONE,
                    null,
                    dailySum,
                    true,
                    totalValue,
                    difference
            );
        }
        return new FestivalVisitorInputSnapshot(
                FestivalVisitorCountInputMode.DAILY,
                FestivalVisitorInputStatus.READY,
                FestivalVisitorEffectiveSource.DAILY_SUM,
                dailySum,
                dailySum,
                totalEntered,
                totalValue,
                difference
        );
    }

    private static FestivalVisitorInputSnapshot resolveTotal(
            boolean totalEntered,
            Integer totalValue,
            Long difference
    ) {
        if (!totalEntered) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.TOTAL,
                    FestivalVisitorInputStatus.PARTIAL,
                    FestivalVisitorEffectiveSource.NONE,
                    null,
                    0L,
                    false,
                    null,
                    difference
            );
        }
        return new FestivalVisitorInputSnapshot(
                FestivalVisitorCountInputMode.TOTAL,
                FestivalVisitorInputStatus.READY,
                FestivalVisitorEffectiveSource.TOTAL,
                totalValue.longValue(),
                0L,
                true,
                totalValue,
                difference
        );
    }

    private static FestivalVisitorInputSnapshot resolveUnset(
            boolean allDaysFilled,
            long dailySum,
            boolean totalEntered,
            Integer totalValue,
            Long difference,
            boolean noDaily
    ) {
        if (allDaysFilled && totalEntered && difference != null && difference != 0L) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.UNSET,
                    FestivalVisitorInputStatus.CONFLICT,
                    FestivalVisitorEffectiveSource.NONE,
                    null,
                    dailySum,
                    true,
                    totalValue,
                    difference
            );
        }
        if (allDaysFilled) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.UNSET,
                    FestivalVisitorInputStatus.READY,
                    FestivalVisitorEffectiveSource.DAILY_SUM,
                    dailySum,
                    dailySum,
                    totalEntered,
                    totalValue,
                    difference
            );
        }
        if (totalEntered) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.UNSET,
                    FestivalVisitorInputStatus.READY,
                    FestivalVisitorEffectiveSource.TOTAL,
                    totalValue.longValue(),
                    dailySum,
                    true,
                    totalValue,
                    difference
            );
        }
        if (noDaily) {
            return new FestivalVisitorInputSnapshot(
                    FestivalVisitorCountInputMode.UNSET,
                    FestivalVisitorInputStatus.UNSET,
                    FestivalVisitorEffectiveSource.NONE,
                    null,
                    0L,
                    false,
                    null,
                    null
            );
        }
        return new FestivalVisitorInputSnapshot(
                FestivalVisitorCountInputMode.UNSET,
                FestivalVisitorInputStatus.PARTIAL,
                FestivalVisitorEffectiveSource.NONE,
                null,
                dailySum,
                false,
                null,
                null
        );
    }

    /**
     * 방문 인원 투트랙 해석 결과이다.
     */
    public record FestivalVisitorInputSnapshot(
            FestivalVisitorCountInputMode inputMode,
            FestivalVisitorInputStatus status,
            FestivalVisitorEffectiveSource source,
            Long effectiveVisitorCount,
            long dailySum,
            boolean totalSaved,
            Integer totalVisitorCount,
            Long difference
    ) {
    }
}
