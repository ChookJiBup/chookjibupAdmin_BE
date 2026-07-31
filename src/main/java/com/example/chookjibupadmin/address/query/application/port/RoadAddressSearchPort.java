package com.example.chookjibupadmin.address.query.application.port;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressSearchResult;

/**
 * 외부 도로명주소 검색 기능의 application 계층 계약이다.
 */
public interface RoadAddressSearchPort {

    /**
     * 검색어와 페이징 조건으로 도로명주소를 조회한다.
     */
    RoadAddressSearchResult search(String keyword, int page, int size);
}
