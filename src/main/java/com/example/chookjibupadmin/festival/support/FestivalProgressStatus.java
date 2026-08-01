package com.example.chookjibupadmin.festival.support;

import java.time.LocalDate;

/**
 * 기준일과 축제 기간으로 구분하는 축제 진행 상태이다.
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
        if (today.isBefore(startDate)) {
            return UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return COMPLETED;
        }
        return ONGOING;
    }
}
