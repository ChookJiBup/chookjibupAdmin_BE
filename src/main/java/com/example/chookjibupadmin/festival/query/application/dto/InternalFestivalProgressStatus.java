package com.example.chookjibupadmin.festival.query.application.dto;

import java.time.LocalDate;

/**
 * 사용자 서버에 노출할 날짜 기준 축제 진행 상태이다.
 */
public enum InternalFestivalProgressStatus {
    UPCOMING,
    ONGOING,
    COMPLETED;

    /**
     * 기준일과 축제 기간으로 진행 상태를 계산한다.
     */
    public static InternalFestivalProgressStatus from(
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
