package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 배치도 이름을 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalMapName {

    private String value;

    private FestivalMapName(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = value.trim();
        if (normalized.length() > 150) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = normalized;
    }

    public static FestivalMapName of(String value) {
        return new FestivalMapName(value);
    }
}
