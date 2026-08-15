package com.example.chookjibupadmin.visitor.command.application.dto;

import java.util.UUID;

/**
 * 총 방문 인원 수 입력 결과이다.
 */
public record FestivalTotalVisitorCountResult(
        UUID festivalId,
        int visitorCount
) {
}
