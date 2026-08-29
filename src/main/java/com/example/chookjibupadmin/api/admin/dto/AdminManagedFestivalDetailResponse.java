package com.example.chookjibupadmin.api.admin.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalDetail;
import com.example.chookjibupadmin.api.festival.dto.FestivalLocationResponse;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 축제 상세 및 수정 화면 응답 DTO이다.
 */
@Schema(description = "관리자 축제 상세 조회 응답")
public record AdminManagedFestivalDetailResponse(
        @Schema(description = "외부 노출용 축제 ID") UUID festivalId,
        @Schema(description = "축제 묶음 ID") UUID seriesId,
        @Schema(description = "축제명") String festivalName,
        @Schema(description = "축제 설명") String description,
        @Schema(description = "개최 연도") int festivalYear,
        @Schema(description = "관리자 역할") AdminRole role,
        @Schema(description = "축제 게시 상태") FestivalStatus festivalStatus,
        @Schema(description = "날짜 기준 진행 상태") FestivalProgressStatus progressStatus,
        @Schema(description = "대표 주소") String address,
        @Schema(description = "대표 상세주소") String detailAddress,
        @Schema(description = "시작일") LocalDate startDate,
        @Schema(description = "종료일") LocalDate endDate,
        @Schema(description = "일일 운영 시작 시간") LocalTime operationStartTime,
        @Schema(description = "일일 운영 종료 시간") LocalTime operationEndTime,
        @Schema(description = "방문 인원 입력 방식", example = "DAILY")
        FestivalVisitorCountInputMode visitorCountInputMode,
        @Schema(description = "축제 장소 목록") List<FestivalLocationResponse> locations
) {

    public static AdminManagedFestivalDetailResponse from(
            AdminManagedFestivalDetail detail
    ) {
        return new AdminManagedFestivalDetailResponse(
                detail.festivalId(),
                detail.seriesId(),
                detail.festivalName(),
                detail.description(),
                detail.festivalYear(),
                detail.role(),
                detail.festivalStatus(),
                detail.progressStatus(),
                detail.address(),
                detail.detailAddress(),
                detail.startDate(),
                detail.endDate(),
                detail.operationStartTime(),
                detail.operationEndTime(),
                detail.visitorCountInputMode(),
                detail.locations().stream()
                        .map(FestivalLocationResponse::from)
                        .toList()
        );
    }
}
