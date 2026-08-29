package com.example.chookjibupadmin.booth.command.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBoothQueueCommand(
        BigDecimal tailLatitude,
        BigDecimal tailLongitude,
        Integer queueTailMeters,
        List<QueuePathPointCommand> path
) {
    public record QueuePathPointCommand(BigDecimal lat, BigDecimal lng) {
    }
}
