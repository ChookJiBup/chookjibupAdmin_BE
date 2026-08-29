package com.example.chookjibupadmin.booth.command.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BoothQueueResult(
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
    public static BoothQueueResult from(BoothQueue queue, String boothName) {
        return new BoothQueueResult(
                queue.getPublicId(),
                queue.getBoothId(),
                boothName,
                queue.getTailLatitude(),
                queue.getTailLongitude(),
                queue.getQueueTailMeters(),
                queue.getPathGeometry(),
                queue.getModifierType(),
                queue.getUpdatedAt()
        );
    }
}
