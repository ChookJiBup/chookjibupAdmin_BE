package com.example.chookjibupadmin.map.query.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 현장 운영 지도 조회 결과.
 *
 * 승인 부스(booths)와 시설 노드(facilities)를 한 배열에 섞지 않는다. 부스는 booth_info와
 * 연결돼 혼잡도·대기열이 붙는 운영 대상이고, 시설은 화장실·입구·출구처럼 위치만 알려주는
 * 참고 표시라 화면에서 다루는 방식이 다르다.
 */
public record FestivalOperationsMapView(
        UUID mapId,
        long editRevision,
        String mapKind,
        MapPresentationView presentation,
        List<ApprovedBoothMarkerView> booths,
        List<FacilityMarkerView> facilities
) {

    public record ApprovedBoothMarkerView(
            Long boothId,
            UUID nodeId,
            String name,
            String nodeType,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }

    /** 부스로 승인되지 않은 확정 POINT 노드(화장실·입구·출구·기타 시설). */
    public record FacilityMarkerView(
            UUID nodeId,
            String name,
            String nodeType,
            BigDecimal lat,
            BigDecimal lng
    ) {
    }
}
