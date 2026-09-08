package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.api.map.dto.MapEditorResponse.PresentationResponse;
import com.example.chookjibupadmin.map.query.application.dto.FestivalOperationsMapView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "현장 운영 지도 조회 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FestivalOperationsMapResponse(
        UUID mapId,
        long editRevision,
        String mapKind,
        PresentationResponse presentation,
        List<BoothMarkerResponse> booths
) {
    public static FestivalOperationsMapResponse from(FestivalOperationsMapView view) {
        return new FestivalOperationsMapResponse(
                view.mapId(),
                view.editRevision(),
                view.mapKind(),
                view.presentation() == null
                        ? null
                        : PresentationResponse.from(view.presentation()),
                view.booths().stream()
                        .map(booth -> new BoothMarkerResponse(
                                booth.boothId(),
                                booth.nodeId(),
                                booth.name(),
                                booth.lat(),
                                booth.lng()
                        ))
                        .toList()
        );
    }

    public record BoothMarkerResponse(
            Long boothId,
            UUID nodeId,
            String name,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
