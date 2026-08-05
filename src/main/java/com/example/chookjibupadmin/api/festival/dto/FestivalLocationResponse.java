package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationSourceType;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record FestivalLocationResponse(
        UUID locationId,
        FestivalLocationType locationType,
        String locationName,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String postalCode,
        String buildingManagementNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        Map<String, Object> boundaryGeometry,
        FestivalLocationSourceType sourceType,
        boolean primary,
        int sortOrder
) {
    public static FestivalLocationResponse from(FestivalLocationDetail location) {
        return new FestivalLocationResponse(
                location.locationId(),
                location.locationType(),
                location.locationName(),
                location.roadAddress(),
                location.jibunAddress(),
                location.detailAddress(),
                location.postalCode(),
                location.buildingManagementNumber(),
                location.latitude(),
                location.longitude(),
                location.boundaryGeometry(),
                location.sourceType(),
                location.primary(),
                location.sortOrder()
        );
    }
}
