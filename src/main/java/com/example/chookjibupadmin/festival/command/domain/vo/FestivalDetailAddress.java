package com.example.chookjibupadmin.festival.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 장소의 선택 상세주소를 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalDetailAddress {

    private String value;

    private FestivalDetailAddress(String value) {
        this.value = normalize(value);
    }

    /**
     * 문자열 상세주소를 검증한 뒤 값 객체로 변환한다.
     */
    public static FestivalDetailAddress of(String value) {
        return new FestivalDetailAddress(value);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return trimmed;
    }
}
