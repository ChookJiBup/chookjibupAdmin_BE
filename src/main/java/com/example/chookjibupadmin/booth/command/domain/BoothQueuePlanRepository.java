package com.example.chookjibupadmin.booth.command.domain;

import java.util.Optional;

/** 사전 계획 저장소. 최초 생성과 변경은 승인 부스 락으로 직렬화한다. */
public interface BoothQueuePlanRepository {
    Optional<BoothQueuePlan> findByBoothId(Long boothId);
    BoothQueuePlan save(BoothQueuePlan plan);
}
