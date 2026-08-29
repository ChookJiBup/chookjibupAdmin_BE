package com.example.chookjibupadmin.dashboard.query.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DashboardBoothView(
        Long boothId,
        String boothName,
        UUID roadmapNodePublicId,
        BigDecimal lat,
        BigDecimal lng,
        String congestionLevel,
        Integer waitMinutes,
        LocalDateTime congestionUpdatedAt,
        String modifierType,
        Long modifierAdminId,
        Long modifierStaffId,
        String modifierName
) {
}
