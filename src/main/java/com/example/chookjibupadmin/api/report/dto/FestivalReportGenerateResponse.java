package com.example.chookjibupadmin.api.report.dto;

import com.example.chookjibupadmin.report.command.application.dto.FestivalReportGenerateResult;
import java.util.UUID;

public record FestivalReportGenerateResponse(
        UUID festivalId,
        UUID jobId,
        String status
) {

    public static FestivalReportGenerateResponse from(
            FestivalReportGenerateResult result
    ) {
        return new FestivalReportGenerateResponse(
                result.festivalId(),
                result.jobId(),
                result.status()
        );
    }
}
