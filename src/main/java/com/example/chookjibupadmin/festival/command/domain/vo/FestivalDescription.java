package com.example.chookjibupadmin.festival.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 설명을 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalDescription {

    private String value;

    private FestivalDescription(String value) {
        this.value = normalize(value);
    }

    /**
     * 문자열 축제 설명을 검증한 뒤 값 객체로 변환한다.
     */
    public static FestivalDescription of(String value) {
        return new FestivalDescription(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (trimmed.length() > 1000) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return trimmed;
    }
}
