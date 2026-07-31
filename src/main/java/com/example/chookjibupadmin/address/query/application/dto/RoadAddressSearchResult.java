package com.example.chookjibupadmin.address.query.application.dto;

import java.util.List;

/**
 * 도로명주소 검색 결과와 페이징 정보를 표현한다.
 */
public record RoadAddressSearchResult(
        int page,
        int size,
        int totalCount,
        List<RoadAddressView> addresses
) {
}
