package com.example.chookjibupadmin.operator.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현장 스태프 표시 이름 값이다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldStaffName {

    private static final int MAX_LENGTH = 100;

    private String value;

    private FieldStaffName(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = trimmed;
    }

    /**
     * 현장 스태프 표시 이름을 생성한다.
     */
    public static FieldStaffName of(String value) {
        return new FieldStaffName(value);
    }
}
