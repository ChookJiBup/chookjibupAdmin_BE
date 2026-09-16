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

        @Schema(description = "호환용 보고 거리(m). 경로/부스 좌표가 있으면 서버 계산을 우선한다.", example = "18")
        @PositiveOrZero
        Integer queueTailMeters,

        /**
         * 대기열 경로.
         * <ul>
         *   <li>필드 생략({@code null}) — 사전 동선 투영 또는 줄끝 기반 계산</li>
         *   <li>빈 배열 — 경로 삭제</li>
         *   <li>2점 이상 — 경로 교체(마지막 점은 줄끝과 동일)</li>
         * </ul>
         */
        @Schema(description = "실제 경로. 생략=동선/줄끝 계산, []=경로 삭제, 2점 이상=교체. 저장 응답에 계산 시간 포함.")
        List<@NotNull @Valid PathPointRequest> path,
        @Schema(description = "조회한 observationRevision. 구 FE 호환을 위해 생략 가능.")
        @PositiveOrZero Long expectedRevision,
        @Schema(description = "참조한 사전 동선 revision. 일치하지 않으면 409.")
        @PositiveOrZero Long planRevision
) {
    /**
     * 경로와 기대 버전을 application 명령으로 변환한다.
     */
    public UpdateBoothQueueCommand toCommand() {
        return new UpdateBoothQueueCommand(
                tailLatitude,
                tailLongitude,
                queueTailMeters,
                path == null
                        ? null
                        : path.stream()
                                .map(point -> new QueuePathPointCommand(point.lat(), point.lng()))
                                .toList(),
                expectedRevision,
                planRevision
        );
    }

    @Schema(description = "대기열 경로 좌표")
    public record PathPointRequest(
            @NotNull @DecimalMin("33.0") @DecimalMax("38.7") BigDecimal lat,
            @NotNull @DecimalMin("124.5") @DecimalMax("132.0") BigDecimal lng
    ) {
    }
}
