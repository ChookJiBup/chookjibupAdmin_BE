package com.example.chookjibupadmin.booth.command.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QueuePlanView(UUID planId, Long boothId, List<Map<String, BigDecimal>> path,
        double lengthMeters, double metersPerPerson, double servedPersonsPerMinute,
        int estimatedCapacity, long revision, UUID sourceNodeId, Long nodeVersion, LocalDateTime updatedAt) {
    public static QueuePlanView from(BoothQueuePlan plan, Long nodeVersion) {
        return new QueuePlanView(plan.getPublicId(), plan.getBoothId(), plan.getPathGeometry(),
                plan.getLengthMeters(), plan.getSettings().getMetersPerPerson(),
                plan.getSettings().getServedPersonsPerMinute(),
                (int) Math.floor(plan.getLengthMeters() / plan.getSettings().getMetersPerPerson()),
                plan.getRevision(), plan.getSourceNodeId(), nodeVersion, plan.getUpdatedAt());
    }
}
