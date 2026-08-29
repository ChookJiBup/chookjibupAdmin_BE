package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 축제 상세 화면에 필요한 기본 정보와 장소 목록이다.
 */
public record AdminManagedFestivalDetail(
        UUID festivalId,
        UUID seriesId,
        String festivalName,
        String description,
        int festivalYear,
        AdminRole role,
        FestivalStatus festivalStatus,
        FestivalProgressStatus progressStatus,
        String address,
        String detailAddress,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime operationStartTime,
        LocalTime operationEndTime,
        FestivalVisitorCountInputMode visitorCountInputMode,
        List<FestivalLocationDetail> locations
) {
}
