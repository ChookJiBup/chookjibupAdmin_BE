package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원하는 배치도 이미지 MIME 타입을 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageContentType {

    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";

    private String value;

    private MapImageContentType(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(JPEG) && !normalized.equals(PNG)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = normalized;
    }

    public static MapImageContentType of(String value) {
        return new MapImageContentType(value);
    }
}
