package com.example.demoadmin.address.query.application.dto;

/**
 * 도로명주소 검색 결과 한 건을 표현한다.
 */
public record RoadAddressView(
        String roadAddress,
        String roadAddressPart1,
        String roadAddressPart2,
        String jibunAddress,
        String zipCode,
        String buildingName,
        String buildingManagementNumber
) {
}
