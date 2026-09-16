package com.example.chookjibupadmin.booth.command.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 부스 대기열 줄끝 수정 명령.
 *
 * <p>{@code path} 의미:
 * <ul>
 *   <li>{@code null} — 사전 동선이 있으면 줄끝까지 투영한다. 없으면 동일 줄끝의 경로만 유지하고 변경 줄끝은 다시 계산한다</li>
 *   <li>빈 목록 — 경로를 비운다. 같은 줄끝에서 거리도 생략한 경로 삭제는 기존 시간·관측 시각을 보존한다</li>
 *   <li>크기 1 — 유효하지 않다</li>
 *   <li>크기 2 이상 — 경로를 교체하며 마지막 점은 줄끝 위경도와 같아야 한다</li>
 * </ul>
 */
public record UpdateBoothQueueCommand(
        BigDecimal tailLatitude,
        BigDecimal tailLongitude,
        Integer queueTailMeters,
        List<QueuePathPointCommand> path,
        Long expectedRevision,
        Long planRevision
) {
    public UpdateBoothQueueCommand(BigDecimal lat, BigDecimal lng, Integer meters,
            List<QueuePathPointCommand> path) {
        this(lat, lng, meters, path, null, null);
    }
    public record QueuePathPointCommand(BigDecimal lat, BigDecimal lng) {
    }
}
