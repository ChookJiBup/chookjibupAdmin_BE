package com.example.chookjibupadmin.booth.query.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record FestivalOperationSuggestionView(
        List<SuggestionItemView> suggestions
) {
    public record SuggestionItemView(
            String suggestionId,
            String title,
            String description,
            List<PathPointView> path
    ) {
    }

    public record PathPointView(BigDecimal lat, BigDecimal lng) {
    }
}
