package com.example.chookjibupadmin.map.command.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 지도 표시 설정(경계·오버레이) 부분 수정 명령이다.
 *
 * <p>{@code clearBoundary}/{@code clearOverlay}가 true면 해당 설정을 제거한다.
 * 오버레이 이미지 자체는 별도 업로드 API로 등록하며, 여기서 {@code assetId}는
 * 이미 업로드된 오버레이를 참조할 때만 사용한다.</p>
 */
public record SaveMapPresentationCommand(
        Boolean clearBoundary,
        BoundaryGeometryCommand boundary,
        Boolean clearOverlay,
        OverlayPresentationCommand overlay
) {

    public record BoundaryGeometryCommand(
            String geometryType,
            String schemaVersion,
            List<LatLngPointCommand> points
    ) {
    }

    public record OverlayPresentationCommand(
            UUID assetId,
            BigDecimal centerLatitude,
            BigDecimal centerLongitude,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees,
            BigDecimal opacity,
            Boolean visible,
            Boolean clipToBoundary
    ) {
    }

    public record LatLngPointCommand(BigDecimal lat, BigDecimal lng) {
    }
}
