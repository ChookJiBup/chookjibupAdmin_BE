package com.example.chookjibupadmin.festival.command.application.dto;

import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record FestivalLocationCommand(
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
        boolean primary,
        int sortOrder
) {
    public FestivalLocationCommand(
            FestivalLocationType type,
            String name,
            String road,
            String jibun,
            String detail,
            String postal,
            String building,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean primary,
            int sortOrder
    ) {
        this(
                null,
                type,
                name,
                road,
                jibun,
                detail,
                postal,
                building,
                latitude,
                longitude,
                null,
                primary,
                sortOrder
        );
    }

    /**
     * 주소만으로 대표 장소를 만드는 레거시 입력이다.
     * 좌표는 채우지 않으며, 등록·수정 검증에서 40013으로 거절된다.
     */
    public static FestivalLocationCommand legacy(String address, String detail) {
        return new FestivalLocationCommand(
                null,
                FestivalLocationType.MAIN_VENUE,
                "메인 행사장",
                address,
                null,
                detail,
                null,
                null,
                null,
                null,
                null,
                true,
                0
        );
    }
}
