package com.example.chookjibupadmin.api.report.dto;

import com.example.chookjibupadmin.report.query.application.dto.FestivalReportSummaryView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "축제 결과 보고서 요약 응답")
public record FestivalReportSummaryResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,
        @Schema(description = "총 방문자 수", example = "0")
        long totalVisitorCount,
        @Schema(description = "최대 동시 방문자 수", example = "0")
        long peakConcurrentVisitorCount,
        @Schema(description = "평균 대기 시간 분", example = "0")
        long averageWaitMinutes,
        @Schema(description = "보고서 생성 시각")
        LocalDateTime generatedAt
) {

    /**
     * 결과 보고서 조회 결과를 API 응답으로 변환한다.
     */
    public static FestivalReportSummaryResponse from(FestivalReportSummaryView view) {
        return new FestivalReportSummaryResponse(
                view.festivalId(),
                view.totalVisitorCount(),
                view.peakConcurrentVisitorCount(),
                view.averageWaitMinutes(),
                view.generatedAt()
        );
    }
}
