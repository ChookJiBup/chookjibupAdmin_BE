package com.example.demoadmin.api.booth.dto;

import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueLineCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "부스 대기 라인 수정 요청")
public record UpdateBoothQueueLineRequest(
        @Schema(description = "라인 순서", example = "1")
        @Positive
        int lineOrder,

        @Schema(description = "라인 표시명", example = "라인 1")
        @NotBlank
        String label,

        @Schema(description = "예상 대기시간 분", example = "10")
        @PositiveOrZero
        int expectedWaitingMinutes,

        @Schema(description = "최대 수용 기준", example = "30")
        @PositiveOrZero
        int maxCapacity,

        @Schema(description = "지도 경로 데이터", example = "[{\"x\":120,\"y\":80}]")
        String pathData,

        @Schema(description = "줄 진입점 데이터", example = "{\"x\":120,\"y\":80}")
        String entryPointData
) {

    public UpdateBoothQueueLineCommand toCommand() {
        return new UpdateBoothQueueLineCommand(
                lineOrder,
                label,
                expectedWaitingMinutes,
                maxCapacity,
                pathData,
                entryPointData
        );
    }
}
