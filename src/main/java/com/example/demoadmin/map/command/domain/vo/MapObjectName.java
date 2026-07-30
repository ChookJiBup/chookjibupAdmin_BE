package com.example.demoadmin.map.command.domain.vo;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapObjectName {

    private static final int MAX_LENGTH = 100;
    private static final String DEFAULT_VALUE = "미확인 객체";

    private String value;

    private MapObjectName(String value) {
        String normalized = normalize(value);
        if (normalized.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = normalized;
    }

    public static MapObjectName of(String value) {
        return new MapObjectName(value);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_VALUE;
        }

        return value;
    }
}
