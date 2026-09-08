package com.example.chookjibupadmin.map.query.application.dto;

import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import java.math.BigDecimal;

/**
 * 배치도 이미지를 카카오맵 위에 얹기 위한 기준값이다.
 *
 * <p>이미지 중심이 놓이는 위경도, 이미지 가로폭이 덮는 실거리(m), 이미지 위쪽이 가리키는
 * 방위각(북=0, 시계방향)을 그대로 내보내 프론트가 오버레이 위치·크기·회전을 계산하게 한다.</p>
 */
public record MapImageAnchorView(
        BigDecimal centerLat,
        BigDecimal centerLng,
        BigDecimal groundWidthMeters,
        BigDecimal rotationDegrees
) {

    /** 앵커가 없는 지도는 null을 그대로 돌려준다. */
    public static MapImageAnchorView from(MapImageAnchor anchor) {
        if (anchor == null) {
            return null;
        }
        return new MapImageAnchorView(
                anchor.getCenterLatitude(),
                anchor.getCenterLongitude(),
                anchor.getGroundWidthMeters(),
                anchor.getRotationDegrees()
        );
    }
}
