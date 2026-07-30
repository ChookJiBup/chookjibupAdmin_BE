package com.example.demoadmin.api.booth.dto;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부스 대기 라인 응답")
public record BoothQueueLineResponse(
        @Schema(description = "대기 라인 ID")
        UUID lineId,
        @Schema(description = "라인 순서")
        int lineOrder,
        @Schema(description = "라인 표시명")
        String label,
        @Schema(description = "예상 대기시간 분")
        int expectedWaitingMinutes,
        @Schema(description = "최대 수용 기준")
        int maxCapacity,
        @Schema(description = "지도 경로 데이터")
        String pathData,
        @Schema(description = "줄 진입점 데이터")
        String entryPointData
) {

    public static BoothQueueLineResponse from(BoothQueueLine queueLine) {
        return new BoothQueueLineResponse(
                queueLine.getPublicId(),
                queueLine.getLineOrder(),
                queueLine.getLabelValue(),
                queueLine.getExpectedWaitingMinutes(),
                queueLine.getMaxCapacity(),
                queueLine.getPathData(),
                queueLine.getEntryPointData()
        );
    }

    public static BoothQueueLineResponse from(BoothQueueLineView view) {
        return new BoothQueueLineResponse(
                view.lineId(),
                view.lineOrder(),
                view.label(),
                view.expectedWaitingMinutes(),
                view.maxCapacity(),
                view.pathData(),
                view.entryPointData()
        );
    }
}
