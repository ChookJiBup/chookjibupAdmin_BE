package com.example.chookjibupadmin.api.visitor.dto;

import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.visitor.query.application.dto.FestivalVisitorCountView;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorEffectiveSource;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputStatus;
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
        @Schema(description = "방문 인원 입력 모드")
        FestivalVisitorCountInputMode visitorCountInputMode,
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
        Integer totalOverrideVisitorCount,
        @Schema(description = "총원 저장 여부")
        boolean totalSaved,
        @Schema(description = "리포트 기준 총원", nullable = true)
        Long effectiveVisitorCount,
        @Schema(description = "리포트 기준 총원 출처")
        FestivalVisitorEffectiveSource effectiveSource,
        @Schema(description = "입력 준비 상태")
        FestivalVisitorInputStatus effectiveStatus,
        @Schema(description = "총원 - 일자합 차이", nullable = true)
        Long difference,
        @Schema(description = "결과리포트 생성 가능 여부")
        boolean reportReadyToGenerate
) {

    public static FestivalVisitorCountResponse from(
            FestivalVisitorCountView view
    ) {
        return new FestivalVisitorCountResponse(
                view.festivalId(),
                view.startDate(),
                view.endDate(),
                view.visitorCountInputMode(),
                view.days().stream()
                        .map(FestivalVisitorDayResponse::from)
                        .toList(),
                view.filledDayCount(),
                view.totalDayCount(),
                view.allDaysFilled(),
                view.sumVisitorCount(),
                view.totalOverrideVisitorCount(),
                view.totalSaved(),
                view.effectiveVisitorCount(),
                view.effectiveSource(),
                view.effectiveStatus(),
                view.difference(),
                view.reportReadyToGenerate()
        );
    }
}
