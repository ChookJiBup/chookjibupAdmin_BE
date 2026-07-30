package com.example.demoadmin.api.admin.dto;

import com.example.demoadmin.admin.command.domain.AdminRole;
import com.example.demoadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.demoadmin.festival.command.domain.FestivalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 관리자 개인 관리 축제 조회 응답 DTO이다.
 */
@Schema(description = "관리자 개인 관리 축제 조회 응답")
public record AdminManagedFestivalResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,

        @Schema(description = "축제명", example = "마포나루 새우젓축제")
        String festivalName,

        @Schema(description = "축제 개최 연도", example = "2026")
        int festivalYear,

        @Schema(description = "관리자 역할", example = "FESTIVAL_OWNER")
        AdminRole role,

        @Schema(description = "축제 상태", example = "DRAFT")
        FestivalStatus festivalStatus,

        @Schema(description = "축제 주소", example = "서울특별시 마포구 월드컵로 243")
        String address,

        @Schema(description = "축제 상세주소", example = "광주비엔날레 전시관")
        String detailAddress,

        @Schema(description = "축제 시작일", example = "2026-10-16")
        LocalDate startDate,

        @Schema(description = "축제 종료일", example = "2026-10-18")
        LocalDate endDate
) {

    /**
     * 관리자 개인 관리 축제 조회 결과를 HTTP 응답 DTO로 변환한다.
     */
    public static AdminManagedFestivalResponse from(
            AdminManagedFestivalView view
    ) {
        return new AdminManagedFestivalResponse(
                view.festivalId(),
                view.festivalName(),
                view.festivalYear(),
                view.role(),
                view.festivalStatus(),
                view.address(),
                view.detailAddress(),
                view.startDate(),
                view.endDate()
        );
    }
}
