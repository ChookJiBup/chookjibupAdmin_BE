package com.example.chookjibupadmin.booth.query.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 축제 기간 중 날짜별 마지막 부스 혼잡 상태이다. */
public record FestivalCongestionHistoryView(
        List<DailyCongestionView> days
) {
    public record DailyCongestionView(
            LocalDate visitDate,
            Integer averageWaitMinutes,
            List<BoothCongestionItemView> booths
    ) {
    }

    public record BoothCongestionItemView(
            Long boothId,
            String boothName,
            BoothCongestionLevel congestionLevel,
            Integer waitMinutes,
            LocalDateTime updatedAt
    ) {
    }
}
