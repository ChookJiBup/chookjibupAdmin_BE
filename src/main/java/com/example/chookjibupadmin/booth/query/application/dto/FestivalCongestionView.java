package com.example.chookjibupadmin.booth.query.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import java.time.LocalDateTime;
import java.util.List;

public record FestivalCongestionView(
        Long festivalId,
        LocalDateTime updatedAt,
        Integer activeQueueCount,
        Integer averageWaitMinutes,
        List<BoothCongestionItemView> booths
) {
    public record BoothCongestionItemView(
            Long boothId,
            String boothName,
            BoothCongestionLevel congestionLevel,
            Integer waitMinutes,
            LocalDateTime updatedAt
    ) {
    }
}
