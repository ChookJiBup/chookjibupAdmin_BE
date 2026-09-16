package com.example.chookjibupadmin.booth.command.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 지도 제약에서 미확정 동선 하나를 추천한다. 결과는 서버에서 다시 검증한다. */
public interface QueuePlanRecommendationPort {
    record Input(Map<String, BigDecimal> start, List<Map<String, BigDecimal>> boundary,
            List<Map<String, Object>> facilities, int targetCapacity, double metersPerPerson) {}
    record Proposal(List<Map<String, BigDecimal>> path, String reason) {}
    Proposal recommend(Input input);
}
