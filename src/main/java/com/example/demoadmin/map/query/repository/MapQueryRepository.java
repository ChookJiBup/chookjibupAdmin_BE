package com.example.demoadmin.map.query.repository;

import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import com.example.demoadmin.map.query.application.dto.MapObjectView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 축제 배치도와 지도 객체를 Projection으로 조회하는 저장소 계약이다.
 */
public interface MapQueryRepository {

    /**
     * 축제와 배치도 외부 ID로 배치도 정보를 조회한다.
     */
    Optional<FestivalMapView> findMapByFestivalIdAndPublicId(
            Long festivalId,
            UUID mapId
    );

    /**
     * 축제와 배치도 외부 ID로 지도 객체 목록을 조회한다.
     */
    List<MapObjectView> findObjectsByFestivalIdAndMapPublicId(
            Long festivalId,
            UUID mapId
    );
}
