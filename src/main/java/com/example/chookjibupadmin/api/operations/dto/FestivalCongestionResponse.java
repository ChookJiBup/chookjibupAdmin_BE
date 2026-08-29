package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalCongestionView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "축제 혼잡도 조회 응답")
public record FestivalCongestionResponse(
        UUID festivalId,
        LocalDateTime updatedAt,
        Integer activeQueueCount,
        Integer averageWaitMinutes,
        List<BoothItem> booths
) {
    public static FestivalCongestionResponse from(
            UUID festivalPublicId,
            FestivalCongestionView view
    ) {
        return new FestivalCongestionResponse(
                festivalPublicId,
                view.updatedAt(),
                view.activeQueueCount(),
                view.averageWaitMinutes(),
                view.booths().stream()
                        .map(item -> new BoothItem(
                                item.boothId(),
                                item.boothName(),
                                item.congestionLevel(),
                                item.waitMinutes(),
                                item.updatedAt()
                        ))
                        .toList()
        );
    }

    @Schema(description = "부스별 최신 혼잡")
    public record BoothItem(
            Long boothId,
            String boothName,
            BoothCongestionLevel congestionLevel,
            Integer waitMinutes,
            LocalDateTime updatedAt
    ) {
    }
}
