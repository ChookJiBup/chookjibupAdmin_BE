package com.example.chookjibupadmin.api.dashboard.dto;

import com.example.chookjibupadmin.dashboard.query.application.dto.DashboardBoothView;
import com.example.chookjibupadmin.dashboard.query.application.dto.DashboardZoneView;
import com.example.chookjibupadmin.dashboard.query.application.dto.FestivalDashboardView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "축제 진행 중 대시보드 응답")
public record FestivalDashboardResponse(
        @Schema(description = "외부 노출용 축제 ID")
        UUID festivalId,
        @Schema(description = "하위 availability OR (deprecated)", deprecated = true)
        boolean dataAvailable,
        @Schema(description = "실시간 현재 방문자 수 가용 여부")
        boolean visitorAvailable,
        @Schema(description = "승인 부스 존재 여부")
        boolean boothAvailable,
        @Schema(description = "부스 혼잡 이력 존재 여부")
        boolean congestionAvailable,
        @Schema(description = "요약 지표 집계 가능 여부")
        boolean summaryAvailable,
        @Schema(description = "운영 상태", example = "ONGOING")
        String operatingStatus,
        @Schema(description = "현재 방문자 수(실시간). 없으면 null", nullable = true)
        Long currentVisitorCount,
        @Schema(description = "활성 대기열 수(혼잡 기반: 대기분>0이고 LOW가 아닌 부스). booth_queue와 별개", nullable = true)
        Long activeQueueCount,
        @Schema(description = "평균 대기 시간(분). 최신 부스 혼잡 평균", nullable = true)
        Long averageWaitMinutes,
        @Schema(description = "혼잡 최신 시각", nullable = true)
        LocalDateTime updatedAt,
        @Schema(description = "승인 부스 목록")
        List<BoothResponse> booths,
        @Schema(description = "지도 구역 목록(로드맵 zones). 없으면 빈 배열")
        List<ZoneResponse> zones
) {

    public static FestivalDashboardResponse from(FestivalDashboardView view) {
        return new FestivalDashboardResponse(
                view.festivalId(),
                view.dataAvailable(),
                view.visitorAvailable(),
                view.boothAvailable(),
                view.congestionAvailable(),
                view.summaryAvailable(),
                view.operatingStatus(),
                view.currentVisitorCount(),
                view.activeQueueCount(),
                view.averageWaitMinutes(),
                view.updatedAt(),
                view.booths().stream().map(BoothResponse::from).toList(),
                view.zones().stream().map(ZoneResponse::from).toList()
        );
    }

    public record BoothResponse(
            Long boothId,
            String boothName,
            @Schema(description = "연결 로드맵 노드 publicId", nullable = true)
            UUID roadmapNodePublicId,
            @Schema(description = "WGS84 위도(좌표맵 POINT만)", nullable = true)
            BigDecimal lat,
            @Schema(description = "WGS84 경도(좌표맵 POINT만)", nullable = true)
            BigDecimal lng,
            String congestionLevel,
            Integer waitMinutes,
            LocalDateTime congestionUpdatedAt,
            @Schema(description = "ADMIN | STAFF", nullable = true)
            String modifierType,
            @Schema(nullable = true)
            Long modifierAdminId,
            @Schema(nullable = true)
            Long modifierStaffId,
            @Schema(description = "마지막 혼잡 수정자 이름", nullable = true)
            String modifierName
    ) {
        static BoothResponse from(DashboardBoothView view) {
            return new BoothResponse(
                    view.boothId(),
                    view.boothName(),
                    view.roadmapNodePublicId(),
                    view.lat(),
                    view.lng(),
                    view.congestionLevel(),
                    view.waitMinutes(),
                    view.congestionUpdatedAt(),
                    view.modifierType(),
                    view.modifierAdminId(),
                    view.modifierStaffId(),
                    view.modifierName()
            );
        }
    }

    public record ZoneResponse(
            UUID zoneId,
            String name,
            int sortOrder,
            List<UUID> boothNodeIds
    ) {
        static ZoneResponse from(DashboardZoneView view) {
            return new ZoneResponse(
                    view.zoneId(),
                    view.name(),
                    view.sortOrder(),
                    view.boothNodeIds()
            );
        }
    }
}
