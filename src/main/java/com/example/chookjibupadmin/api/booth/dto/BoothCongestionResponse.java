package com.example.chookjibupadmin.api.booth.dto;

import com.example.chookjibupadmin.booth.command.application.dto.BoothCongestionResult;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "부스 혼잡 저장 응답")
public record BoothCongestionResponse(
        Long boothId,
        BoothCongestionLevel congestionLevel,
        int waitMinutes,
        LocalDateTime createdAt
) {
    public static BoothCongestionResponse from(BoothCongestionResult result) {
        return new BoothCongestionResponse(
                result.boothId(),
                result.congestionLevel(),
                result.waitMinutes(),
                result.createdAt()
        );
    }
}
