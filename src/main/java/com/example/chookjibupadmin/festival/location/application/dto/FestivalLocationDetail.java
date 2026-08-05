package com.example.chookjibupadmin.festival.location.application.dto;

import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationSourceType;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 축제 장소 조회 결과를 전달한다.
 */
public record FestivalLocationDetail(
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

    public static FestivalLocationDetail from(FestivalLocation location) {
        return new FestivalLocationDetail(
                location.getPublicId(),
                location.getLocationType(),
                location.getLocationName(),
                location.getRoadAddress(),
                location.getJibunAddress(),
                location.getDetailAddress(),
                location.getPostalCode(),
                location.getBuildingManagementNumber(),
                location.getLatitude(),
                location.getLongitude(),
                location.getBoundaryGeometry(),
                location.getSourceType(),
                location.isPrimary(),
                location.getSortOrder()
        );
    }
}
