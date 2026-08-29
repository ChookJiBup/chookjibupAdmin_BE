package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "대기열 줄끝 수정 응답")
public record FestivalQueueResponse(
        UUID queueId,
        Long boothId,
        String boothName,
        BigDecimal tailLatitude,
        BigDecimal tailLongitude,
        Integer queueTailMeters,
        List<Map<String, BigDecimal>> path,
        BoothQueueModifierType lastModifierType,
        LocalDateTime updatedAt
) {
    public static FestivalQueueResponse from(BoothQueueResult result) {
        return new FestivalQueueResponse(
                result.queueId(),
                result.boothId(),
                result.boothName(),
                result.tailLatitude(),
                result.tailLongitude(),
                result.queueTailMeters(),
                result.path(),
                result.lastModifierType(),
                result.updatedAt()
        );
    }
}
