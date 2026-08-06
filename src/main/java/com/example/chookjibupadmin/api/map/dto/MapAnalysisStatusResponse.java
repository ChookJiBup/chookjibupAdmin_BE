package com.example.chookjibupadmin.api.map.dto;

import com.example.chookjibupadmin.map.query.application.dto.MapAnalysisStatusView;
import java.time.LocalDateTime;
import java.util.UUID;
public record MapAnalysisStatusResponse(
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

    public static MapAnalysisStatusResponse from(
            MapAnalysisStatusView view
    ) {
        return new MapAnalysisStatusResponse(
                view.jobId(),
                view.status(),
                view.attemptCount(),
                view.detectedCount(),
                view.acceptedCount(),
                view.rejectedCount(),
                view.failureCode(),
                view.failureMessage(),
                view.startedAt(),
                view.completedAt()
        );
    }
}
