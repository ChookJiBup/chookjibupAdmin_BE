package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalCongestionHistoryView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FestivalCongestionHistoryResponse(
        UUID festivalId,
        List<DailyItem> days
) {
    public static FestivalCongestionHistoryResponse from(
            UUID festivalId,
            FestivalCongestionHistoryView view
    ) {
        return new FestivalCongestionHistoryResponse(
                festivalId,
                view.days().stream()
                        .map(day -> new DailyItem(
                                day.visitDate(),
                                day.averageWaitMinutes(),
                                day.booths().stream()
                                        .map(booth -> new BoothItem(
                                                booth.boothId(),
                                                booth.boothName(),
                                                booth.congestionLevel(),
                                                booth.waitMinutes(),
                                                booth.updatedAt()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    public record DailyItem(
            LocalDate visitDate,
            Integer averageWaitMinutes,
            List<BoothItem> booths
    ) {
    }

    public record BoothItem(
            Long boothId,
            String boothName,
            BoothCongestionLevel congestionLevel,
            Integer waitMinutes,
            LocalDateTime updatedAt
    ) {
    }
}
