package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "축제 기본 정보 생성 응답")
public record CreateFestivalResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,
        @Schema(description = "같은 축제를 연도별로 묶는 축제 묶음 UUID", example = "22222222-2222-2222-2222-222222222222")
        UUID seriesId,
        @Schema(description = "개최 연도", example = "2026")
        int year,
        @Schema(description = "축제명", example = "마포나루 새우젓축제")
        String name,
        @Schema(description = "축제 시작일", example = "2026-10-16")
        LocalDate startDate,
        @Schema(description = "축제 종료일", example = "2026-10-18")
        LocalDate endDate,
        @Schema(description = "축제 상태", example = "DRAFT")
        String status,
        @Schema(description = "운영 시작 시간", example = "10:00:00")
        LocalTime operationStartTime,
        @Schema(description = "운영 종료 시간", example = "21:00:00")
        LocalTime operationEndTime,
        @Schema(description = "축제 장소 목록") List<FestivalLocationResponse> locations,
        @Schema(
                description = "축제 현장 리뷰 QR코드가 가리킬 URL(사용자 프런트 기준). "
                        + "QR 이미지 자체가 필요하면 /api/admin/me/managed-festivals/{festivalId}/review-qr을 호출한다.",
                example = "https://user.chookjibup.store/festivals/11111111-1111-1111-1111-111111111111/review?source=qr"
        )
        String reviewQrUrl
) {

    public static CreateFestivalResponse from(Festival festival, String reviewQrUrl) {
        return new CreateFestivalResponse(
                festival.getPublicId(),
                festival.getSeriesPublicId(),
                festival.getYear(),
                festival.getNameValue(),
                festival.getStartDate(),
                festival.getEndDate(),
                festival.getStatus().name(),
                festival.getOperationStartTime(),
                festival.getOperationEndTime(),
                List.of(),
                reviewQrUrl
        );
    }

    public static CreateFestivalResponse from(
            Festival festival,
            List<FestivalLocationDetail> locations,
            String reviewQrUrl
    ) {
        CreateFestivalResponse base = from(festival, reviewQrUrl);
        return new CreateFestivalResponse(
                base.festivalId(),
                base.seriesId(),
                base.year(),
                base.name(),
                base.startDate(),
                base.endDate(),
                base.status(),
                base.operationStartTime(),
                base.operationEndTime(),
                locations.stream().map(FestivalLocationResponse::from).toList(),
                base.reviewQrUrl()
        );
    }
}