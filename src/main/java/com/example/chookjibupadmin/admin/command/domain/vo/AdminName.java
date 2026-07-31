package com.example.chookjibupadmin.admin.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 이름을 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminName {

    private String value;

    private AdminName(String value) {
        this.value = normalize(value);
    }

    /**
     * 문자열 이름을 검증한 뒤 값 객체로 변환한다.
     */
    public static AdminName of(String value) {
        return new AdminName(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return trimmed;
    }
}
