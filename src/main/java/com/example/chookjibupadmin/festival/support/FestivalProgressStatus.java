package com.example.chookjibupadmin.festival.support;

import java.time.LocalDate;

/**
 * 날짜 자동 계산과 관리자 직접 지정에 사용하는 축제 진행 상태이다.
 */
public enum FestivalProgressStatus {
    UPCOMING,
    ONGOING,
    COMPLETED;

    /**
     * 시작일과 종료일을 포함하는 날짜 경계로 진행 상태를 계산한다.
     */
    public static FestivalProgressStatus from(
            LocalDate today,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            return null;
        }
        if (today.isBefore(startDate)) {
            return UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return COMPLETED;
        }
        return ONGOING;
    }

    /** DB에 저장된 상태를 우선하며, 아직 상태가 없는 자료만 날짜로 보완한다. */
    public static FestivalProgressStatus resolve(String stored, LocalDate today,
            LocalDate startDate, LocalDate endDate) {
        return stored == null ? from(today, startDate, endDate)
                : valueOf(stored.toUpperCase(java.util.Locale.ROOT));
    }
}
