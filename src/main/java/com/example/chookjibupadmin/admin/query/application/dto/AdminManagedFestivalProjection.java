package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 관리 축제 DB 조회 결과와 저장된 진행 상태 projection이다.
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
        LocalDate endDate,
        String progressStatus
) {

    /**
     * 저장된 진행 상태를 우선 사용하며, 없는 경우에만 조회 기준일로 보완한다.
     */
    public AdminManagedFestivalView toView(LocalDate today) {
        return new AdminManagedFestivalView(
                festivalId,
                festivalName,
                festivalYear,
                role,
                festivalStatus,
                FestivalProgressStatus.resolve(progressStatus, today, startDate, endDate),
                address,
                detailAddress,
                startDate,
                endDate
        );
    }
}
