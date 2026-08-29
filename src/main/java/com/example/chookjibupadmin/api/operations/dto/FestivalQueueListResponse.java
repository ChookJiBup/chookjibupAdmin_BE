package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalQueueListView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "축제 대기열 목록 응답")
public record FestivalQueueListResponse(
        UUID festivalId,
        List<QueueItem> queues
) {
    public static FestivalQueueListResponse from(
            UUID festivalPublicId,
            FestivalQueueListView view
    ) {
        return new FestivalQueueListResponse(
                festivalPublicId,
                view.queues().stream().map(QueueItem::from).toList()
        );
    }

    @Schema(description = "부스 대기열")
    public record QueueItem(
            UUID queueId,
            Long boothId,
            String boothName,
            BigDecimal tailLatitude,
            BigDecimal tailLongitude,
            Integer queueTailMeters,
            List<Map<String, BigDecimal>> path,
            BoothQueueModifierType lastModifierType,
            @Schema(description = "마지막으로 줄끝을 갱신한 사람의 이름", nullable = true)
            String lastModifierName,
            LocalDateTime updatedAt
    ) {
        public static QueueItem from(BoothQueueResult result) {
            return new QueueItem(
                    result.queueId(),
                    result.boothId(),
                    result.boothName(),
                    result.tailLatitude(),
                    result.tailLongitude(),
                    result.queueTailMeters(),
                    result.path(),
                    result.lastModifierType(),
                    result.lastModifierName(),
                    result.updatedAt()
            );
        }
    }
}
