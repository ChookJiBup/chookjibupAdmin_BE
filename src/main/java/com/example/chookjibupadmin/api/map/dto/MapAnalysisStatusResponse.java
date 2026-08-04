package com.example.chookjibupadmin.api.map.dto;
import com.example.chookjibupadmin.map.query.application.dto.MapAnalysisStatusView;
import java.time.LocalDateTime;
import java.util.UUID;
public record MapAnalysisStatusResponse(UUID jobId,String status,int attemptCount,int detectedCount,
        int acceptedCount,int rejectedCount,String failureCode,String failureMessage,
        LocalDateTime startedAt,LocalDateTime completedAt){
    public static MapAnalysisStatusResponse from(MapAnalysisStatusView v){return new MapAnalysisStatusResponse(
            v.jobId(),v.status(),v.attemptCount(),v.detectedCount(),v.acceptedCount(),v.rejectedCount(),
            v.failureCode(),v.failureMessage(),v.startedAt(),v.completedAt());}
}
