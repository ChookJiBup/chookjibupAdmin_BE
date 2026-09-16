package com.example.chookjibupadmin.booth.command.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 기대 계획 revision과 지도 부스 version은 변경 전 조회값이다. */
public record SaveQueuePlanCommand(List<Map<String, BigDecimal>> path,
        double metersPerPerson, double servedPersonsPerMinute, UUID sourceNodeId,
        long expectedRevision, Long expectedNodeVersion) {}
