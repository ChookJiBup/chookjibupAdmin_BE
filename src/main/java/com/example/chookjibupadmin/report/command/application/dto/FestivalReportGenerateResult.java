package com.example.chookjibupadmin.report.command.application.dto;

import java.util.UUID;

/**
 * 결과 보고서 생성 요청 결과이다.
 */
public record FestivalReportGenerateResult(
        UUID festivalId,
        UUID jobId,
        String status
) {
}
