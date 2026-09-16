package com.example.chookjibupadmin.api.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "새 사전 동선 AI 추천. 결과는 미확정이며 별도 저장이 필요하다.")
public record RecommendQueuePlanRequest(@Min(1) @Max(1000) int targetCapacity,
        @DecimalMin("0.2") @DecimalMax("5") double metersPerPerson) {}
