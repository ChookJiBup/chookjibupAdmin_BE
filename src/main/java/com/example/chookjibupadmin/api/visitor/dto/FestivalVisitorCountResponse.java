package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.visitor.query.application.dto.FestivalVisitorCountView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "축제 방문 인원 입력 현황 응답")
public record FestivalVisitorCountResponse(
        @Schema(description = "외부 노출용 축제 ID")
        UUID festivalId,
        @Schema(description = "축제 시작일")
        LocalDate startDate,
        @Schema(description = "축제 종료일")
        LocalDate endDate,
        @Schema(description = "일자별 방문 인원")
        List<FestivalVisitorDayResponse> days,
        @Schema(description = "입력 완료 일수", example = "1")
        int filledDayCount,
        @Schema(description = "전체 일수", example = "3")
        int totalDayCount,
        @Schema(description = "전 일자 입력 완료 여부", example = "false")
        boolean allDaysFilled,
        @Schema(description = "일자별 합계", example = "1200")
        int sumVisitorCount,
        @Schema(description = "총원 테이블 값, 없으면 null", nullable = true)
        Integer totalOverrideVisitorCount
) {

    public static FestivalVisitorCountResponse from(
            FestivalVisitorCountView view
    ) {
        return new FestivalVisitorCountResponse(
                view.festivalId(),
                view.startDate(),
                view.endDate(),
                view.days().stream()
                        .map(FestivalVisitorDayResponse::from)
                        .toList(),
                view.filledDayCount(),
                view.totalDayCount(),
                view.allDaysFilled(),
                view.sumVisitorCount(),
                view.totalOverrideVisitorCount()
        );
    }
}
