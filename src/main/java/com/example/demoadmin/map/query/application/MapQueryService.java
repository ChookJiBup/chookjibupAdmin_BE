package com.example.demoadmin.map.query.application;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import com.example.demoadmin.map.query.application.dto.MapObjectView;
import com.example.demoadmin.map.query.repository.MapQueryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치도 Query Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryService {

    private final MapQueryRepository mapQueryRepository;

    /**
     * 축제와 배치도 외부 ID로 배치도 정보를 조회한다.
     */
    public FestivalMapView getMap(
            Long festivalId,
            UUID mapId
    ) {
        return mapQueryRepository.findMapByFestivalIdAndPublicId(
                        festivalId,
                        mapId
                )
                .orElseThrow(() -> new CustomException(
                        ErrorCode.FESTIVAL_MAP_NOT_FOUND
                ));
    }

    /**
     * 축제와 배치도 외부 ID로 지도 객체 목록을 조회한다.
     */
    public List<MapObjectView> getObjects(
            Long festivalId,
            UUID mapId
    ) {
        return mapQueryRepository.findObjectsByFestivalIdAndMapPublicId(
                festivalId,
                mapId
        );
    }
}
