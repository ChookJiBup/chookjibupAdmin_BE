package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 파일 저장소에서 이미지를 식별하는 논리 경로를 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageObjectKey {

    private String value;

    private MapImageObjectKey(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = value.trim();
        boolean unsafeSegment = Arrays.stream(normalized.split("/", -1))
                .anyMatch(segment -> segment.isBlank()
                        || segment.equals(".")
                        || segment.equals(".."));
        if (normalized.length() > 700
                || normalized.startsWith("/")
                || normalized.indexOf('\\') >= 0
                || normalized.chars().anyMatch(Character::isISOControl)
                || unsafeSegment) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = normalized;
    }

    public static MapImageObjectKey of(String value) {
        return new MapImageObjectKey(value);
    }
}
