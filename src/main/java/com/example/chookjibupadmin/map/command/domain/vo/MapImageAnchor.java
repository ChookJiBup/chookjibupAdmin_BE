package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배치도 이미지를 실세계 위경도에 고정하는 기준값이다.
 *
 * <p>이미지 중심이 놓이는 위경도, 이미지 가로폭이 덮는 실거리(m), 이미지 위쪽이 가리키는
 * 방위각(북=0, 시계방향)만으로 이미지 정규화 좌표(schema 1.0)를 WGS84(schema 2.0)로 옮길 수 있다.
 * 네 귀퉁이 좌표나 GCP를 따로 두지 않은 이유는, 관리자가 조정할 값을 최소로 줄여
 * 앵커 편집 UI 없이도 기본값으로 곧장 동작시키기 위해서다.</p>
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageAnchor {

    /** 앵커 UI가 없는 동안 쓰는 기본 이미지 가로폭(m). 중형 축제 부지 한 변에 해당한다. */
    public static final BigDecimal DEFAULT_GROUND_WIDTH_METERS =
            new BigDecimal("300.00");

    /** 앵커 UI가 없는 동안 쓰는 기본 방위각. 이미지 위쪽을 북쪽으로 본다. */
    public static final BigDecimal DEFAULT_ROTATION_DEGREES =
            new BigDecimal("0.000");

    private static final BigDecimal MAX_GROUND_WIDTH_METERS =
            new BigDecimal("100000.00");

    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private BigDecimal groundWidthMeters;
    private BigDecimal rotationDegrees;

    private MapImageAnchor(
            BigDecimal centerLatitude,
            BigDecimal centerLongitude,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees
    ) {
        if (centerLatitude == null || centerLongitude == null
                || groundWidthMeters == null || rotationDegrees == null
                || centerLatitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || centerLongitude.abs().compareTo(BigDecimal.valueOf(180)) > 0
                || groundWidthMeters.signum() <= 0
                || groundWidthMeters.compareTo(MAX_GROUND_WIDTH_METERS) > 0
                || rotationDegrees.abs().compareTo(BigDecimal.valueOf(360)) > 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.centerLatitude = centerLatitude.setScale(7, RoundingMode.HALF_UP);
        this.centerLongitude = centerLongitude.setScale(7, RoundingMode.HALF_UP);
        this.groundWidthMeters = groundWidthMeters.setScale(2, RoundingMode.HALF_UP);
        this.rotationDegrees = rotationDegrees.setScale(3, RoundingMode.HALF_UP);
    }

    public static MapImageAnchor of(
            BigDecimal centerLatitude,
            BigDecimal centerLongitude,
            BigDecimal groundWidthMeters,
            BigDecimal rotationDegrees
    ) {
        return new MapImageAnchor(
                centerLatitude,
                centerLongitude,
                groundWidthMeters,
                rotationDegrees
        );
    }

    /** 축제 대표 위치를 중심으로 기본 폭·방위각을 적용한 앵커를 만든다. */
    public static MapImageAnchor defaultAt(
            BigDecimal centerLatitude,
            BigDecimal centerLongitude
    ) {
        return new MapImageAnchor(
                centerLatitude,
                centerLongitude,
                DEFAULT_GROUND_WIDTH_METERS,
                DEFAULT_ROTATION_DEGREES
        );
    }
}
