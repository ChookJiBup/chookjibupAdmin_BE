package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 관리 축제 DB 조회 결과에 날짜 기준 진행 상태를 더하기 전의 projection이다.
 */
public record AdminManagedFestivalProjection(
        UUID festivalId,
        String festivalName,
        int festivalYear,
        AdminRole role,
        FestivalStatus festivalStatus,
        String address,
        String detailAddress,
        LocalDate startDate,
        LocalDate endDate
) {

    /**
     * 조회 요청의 기준일로 진행 상태가 포함된 View를 생성한다.
     */
    public AdminManagedFestivalView toView(LocalDate today) {
        return new AdminManagedFestivalView(
                festivalId,
                festivalName,
                festivalYear,
                role,
                festivalStatus,
                FestivalProgressStatus.from(today, startDate, endDate),
                address,
                detailAddress,
                startDate,
                endDate
        );
    }
}
