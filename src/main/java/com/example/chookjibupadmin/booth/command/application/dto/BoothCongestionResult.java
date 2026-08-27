package com.example.chookjibupadmin.booth.command.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import java.time.LocalDateTime;

public record BoothCongestionResult(
        Long boothId,
        BoothCongestionLevel congestionLevel,
        int waitMinutes,
        LocalDateTime createdAt
) {

    public static BoothCongestionResult from(BoothCongestion congestion) {
        return new BoothCongestionResult(
                congestion.getBoothId(),
                congestion.getCongestionLevel(),
                congestion.getWaitMinutes() == null ? 0 : congestion.getWaitMinutes(),
                congestion.getCreatedAt()
        );
    }
}
