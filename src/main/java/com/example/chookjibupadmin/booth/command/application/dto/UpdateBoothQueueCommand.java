package com.example.chookjibupadmin.booth.command.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 부스 대기열 줄끝 수정 명령.
 *
 * <p>{@code path} 의미:
 * <ul>
 *   <li>{@code null} — 기존 {@code pathGeometry}를 유지한다</li>
 *   <li>빈 목록 — {@code pathGeometry}를 null로 비운다</li>
 *   <li>크기 1 — 유효하지 않다</li>
 *   <li>크기 2 이상 — 경로를 교체하며 마지막 점은 줄끝 위경도와 같아야 한다</li>
 * </ul>
 */
public record UpdateBoothQueueCommand(
        BigDecimal tailLatitude,
        BigDecimal tailLongitude,
        Integer queueTailMeters,
        List<QueuePathPointCommand> path
) {
    public record QueuePathPointCommand(BigDecimal lat, BigDecimal lng) {
    }
}
