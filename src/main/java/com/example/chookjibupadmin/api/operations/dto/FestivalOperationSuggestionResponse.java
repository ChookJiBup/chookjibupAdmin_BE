package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.query.application.dto.FestivalOperationSuggestionView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "축제 운영 AI/규칙 제안 목록")
public record FestivalOperationSuggestionResponse(
        List<SuggestionItem> suggestions
) {
    public static FestivalOperationSuggestionResponse from(
            FestivalOperationSuggestionView view
    ) {
        return new FestivalOperationSuggestionResponse(
                view.suggestions().stream()
                        .map(item -> new SuggestionItem(
                                item.suggestionId(),
                                item.title(),
                                item.description(),
                                item.path() == null
                                        ? List.of()
                                        : item.path().stream()
                                                .map(p -> new PathPoint(p.lat(), p.lng()))
                                                .toList()
                        ))
                        .toList()
        );
    }

    @Schema(description = "운영 제안")
    public record SuggestionItem(
            String suggestionId,
            String title,
            String description,
            List<PathPoint> path
    ) {
    }

    @Schema(description = "지도 경로 점")
    public record PathPoint(BigDecimal lat, BigDecimal lng) {
    }
}
