package com.example.chookjibupadmin.visitor.support;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 축제 기간의 일자별 방문 인원 입력 완료 여부를 계산한다.
 */
public final class FestivalVisitorDaySupport {

    private FestivalVisitorDaySupport() {
    }

    public static int totalDayCount(Festival festival) {
        LocalDate start = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        return (int) (end.toEpochDay() - start.toEpochDay() + 1);
    }

    /**
     * 축제 기간(시작일~종료일)에 속하는 일자별 행만 남긴다.
     */
    public static List<FestivalDailyVisitorCount> withinFestivalPeriod(
            Festival festival,
            List<FestivalDailyVisitorCount> dailyCounts
    ) {
        LocalDate start = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        return dailyCounts.stream()
                .filter(daily -> {
                    LocalDate visitDate = daily.getVisitDate();
                    return visitDate != null
                            && !visitDate.isBefore(start)
                            && !visitDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    public static boolean isAllDaysFilled(
            Festival festival,
            List<FestivalDailyVisitorCount> dailyCounts
    ) {
        Set<LocalDate> savedDates = new HashSet<>();
        for (FestivalDailyVisitorCount daily : withinFestivalPeriod(festival, dailyCounts)) {
            savedDates.add(daily.getVisitDate());
        }

        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            if (!savedDates.contains(cursor)) {
                return false;
            }
            cursor = cursor.plusDays(1);
        }
        return true;
    }

    /**
     * 일자별 전 기간 입력 완료이거나 총원 입력이 있으면 리포트 생성 가능하다.
     */
    public static boolean isVisitorInputReady(
            Festival festival,
            List<FestivalDailyVisitorCount> dailyCounts,
            boolean totalEntered
    ) {
        return totalEntered || isAllDaysFilled(festival, dailyCounts);
    }
}
