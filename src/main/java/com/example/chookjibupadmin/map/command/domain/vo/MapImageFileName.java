package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드 당시 이미지 파일명을 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageFileName {

    private String value;

    private MapImageFileName(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = value.trim();
        if (normalized.length() > 255
                || normalized.equals(".")
                || normalized.equals("..")
                || normalized.indexOf('/') >= 0
                || normalized.indexOf('\\') >= 0
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = normalized;
    }

    public static MapImageFileName of(String value) {
        return new MapImageFileName(value);
    }
}
