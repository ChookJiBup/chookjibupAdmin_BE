package com.example.chookjibupadmin.report.command.application.dto;

import java.util.UUID;

/**
 * 제한 시간을 넘겨 회수한 결과 보고서 생성 작업의 처리 결과이다.
 */
public record FestivalReportJobReleaseResult(
        UUID jobPublicId,
        Long festivalId,
        boolean retrying
) {
}
