package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 관리자 개인 관리 축제 조회 결과를 표현한다.
 */
public record AdminManagedFestivalView(
        UUID festivalId,
        String festivalName,
        int festivalYear,
        AdminRole role,
        FestivalStatus festivalStatus,
        FestivalProgressStatus progressStatus,
        String address,
        String detailAddress,
        LocalDate startDate,
        LocalDate endDate
) {
}
