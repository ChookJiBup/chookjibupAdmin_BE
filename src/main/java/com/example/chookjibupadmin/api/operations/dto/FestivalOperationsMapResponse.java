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
        @Schema(description = "승인 부스 마커")
        List<BoothMarkerResponse> booths,
        @Schema(description = "부스가 아닌 확정 시설 마커(화장실·입구·출구 등). 없으면 빈 배열")
        List<FacilityMarkerResponse> facilities
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
                                booth.nodeType(),
                                booth.lat(),
                                booth.lng()
                        ))
                        .toList(),
                view.facilities().stream()
                        .map(facility -> new FacilityMarkerResponse(
                                facility.nodeId(),
                                facility.name(),
                                facility.nodeType(),
                                facility.lat(),
                                facility.lng()
                        ))
                        .toList()
        );
    }

    public record BoothMarkerResponse(
            Long boothId,
            UUID nodeId,
            String name,
            @Schema(description = "노드 유형. 부스로 승인된 노드라 보통 BOOTH", example = "BOOTH")
            String nodeType,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }

    @Schema(description = "부스가 아닌 시설 마커")
    public record FacilityMarkerResponse(
            UUID nodeId,
            String name,
            @Schema(description = "노드 유형", example = "RESTROOM")
            String nodeType,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
