package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화면 좌표 기준 이미지 너비와 높이를 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageDimensions {

    private int width;
    private int height;

    private MapImageDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.width = width;
        this.height = height;
    }

    public static MapImageDimensions of(int width, int height) {
        return new MapImageDimensions(width, height);
    }
}
