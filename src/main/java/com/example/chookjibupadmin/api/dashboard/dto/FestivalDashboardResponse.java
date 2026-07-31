package com.example.chookjibupadmin.api.dashboard.dto;

import com.example.chookjibupadmin.dashboard.query.application.dto.FestivalDashboardView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "축제 진행 중 대시보드 응답")
public record FestivalDashboardResponse(
        @Schema(description = "외부 노출용 축제 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,
        @Schema(description = "운영 상태", example = "PREPARING")
        String operatingStatus,
        @Schema(description = "현재 방문자 수", example = "0")
        long currentVisitorCount,
        @Schema(description = "활성 대기열 수", example = "0")
        long activeQueueCount,
        @Schema(description = "평균 대기 시간 분", example = "0")
        long averageWaitMinutes,
        @Schema(description = "대시보드 갱신 시각")
        LocalDateTime updatedAt
) {

    /**
     * 대시보드 조회 결과를 API 응답으로 변환한다.
     */
    public static FestivalDashboardResponse from(FestivalDashboardView view) {
        return new FestivalDashboardResponse(
                view.festivalId(),
                view.operatingStatus(),
                view.currentVisitorCount(),
                view.activeQueueCount(),
                view.averageWaitMinutes(),
                view.updatedAt()
        );
    }
}
