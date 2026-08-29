package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand.QueuePathPointCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "대기열 줄끝 수정 요청")
public record UpdateFestivalQueueRequest(
        @Schema(description = "줄끝 위도", example = "37.5665")
        @NotNull
        @DecimalMin("33.0")
        @DecimalMax("38.7")
        BigDecimal tailLatitude,

        @Schema(description = "줄끝 경도", example = "126.9780")
        @NotNull
        @DecimalMin("124.5")
        @DecimalMax("132.0")
        BigDecimal tailLongitude,

        @Schema(description = "줄끝까지 거리(m). 없으면 null", example = "18")
        @PositiveOrZero
        Integer queueTailMeters,

        @Schema(description = "대기열 경로 점 목록(선택)")
        List<@Valid PathPointRequest> path
) {
    public UpdateBoothQueueCommand toCommand() {
        return new UpdateBoothQueueCommand(
                tailLatitude,
                tailLongitude,
                queueTailMeters,
                path == null
                        ? null
                        : path.stream()
                                .map(point -> new QueuePathPointCommand(point.lat(), point.lng()))
                                .toList()
        );
    }

    @Schema(description = "대기열 경로 좌표")
    public record PathPointRequest(
            @NotNull @DecimalMin("33.0") @DecimalMax("38.7") BigDecimal lat,
            @NotNull @DecimalMin("124.5") @DecimalMax("132.0") BigDecimal lng
    ) {
    }
}
