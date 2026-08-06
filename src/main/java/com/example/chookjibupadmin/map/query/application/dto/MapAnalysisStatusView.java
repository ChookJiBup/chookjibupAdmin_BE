package com.example.chookjibupadmin.map.query.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;
public record MapAnalysisStatusView(
        UUID jobId,
        String status,
        int attemptCount,
        int detectedCount,
        int acceptedCount,
        int rejectedCount,
        String failureCode,
        String failureMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
