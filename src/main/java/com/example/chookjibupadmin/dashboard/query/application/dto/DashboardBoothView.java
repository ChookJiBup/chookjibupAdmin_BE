package com.example.chookjibupadmin.dashboard.query.application.dto;

import java.time.LocalDateTime;

public record DashboardBoothView(
        Long boothId,
        String boothName,
        String congestionLevel,
        Integer waitMinutes,
        LocalDateTime congestionUpdatedAt
) {
}
