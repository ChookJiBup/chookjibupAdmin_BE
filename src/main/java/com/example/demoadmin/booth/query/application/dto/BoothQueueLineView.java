package com.example.demoadmin.booth.query.application.dto;

import java.util.UUID;

public record BoothQueueLineView(
        UUID lineId,
        int lineOrder,
        String label,
        int expectedWaitingMinutes,
        int maxCapacity,
        String pathData,
        String entryPointData
) {
}
